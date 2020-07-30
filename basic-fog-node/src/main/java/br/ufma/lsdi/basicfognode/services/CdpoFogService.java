package br.ufma.lsdi.basicfognode.services;

import br.ufma.lsdi.basicfognode.models.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

/*
Esta classe é responsável por toda a comunicação com o CDPO
 */
@Service
public class CdpoFogService {

    // MQTT topics
    private final String EDGE_KEEP_ALIVE_TOPIC = "/cdpo/edge/keep-alive";
    private final String CDPO_EDGE_RULE = "/cdpo/edge-rule/";
    private final String CDPO_EDGE_EVENT = "/cdpo/edge-event/";
    private final String CDPO_DEPLOY_STATUS = "/cdpo/deploy-status/";
    private final String CDPO_COMPOSER_PUBLISH_EVENT = "/cdpo/publishNewCdpoEvent/";

    // cdpo end points
    private final String IOT_CATALOGUER_GATEWAY = "iot-cataloguer/gateway";
    private final String IOT_CATALOGUER_GATEWAY_RELATE = "/iot-cataloguer/gateway/relate";
    private final String EPN_DEPLOY_STATUS = "/epn/deployStatus";

    private final String HEADER_DN_KEY = "X-SSL-Client-DN";


    @Value("${cdpo.mqttbroker.url}")
    private String brokerUrl;

    @Value("${cdpo.fog.dn}")
    private String dn;

    @Value("${cdpo.url}")
    private String cdpoUrl;

    @Value("${cdpo.iotcataloguer.url}")
    private String iotCataloguerUrl;

    @Value("${cdpo.fognode.url}")
    private String fogNodeUrl;

    @Value("${cdpo.composer.url}")
    private String cdpoComposerUrl;

    private final CepService cepService;
    private MqttClient mqttClient;

    public CdpoFogService(CepService cepService) {
        this.cepService = cepService;
    }

    /*
    Inicializa o serviço MQTT
     */
    @PostConstruct
    public void post() {
        initMqtt();
        sendKeepAlive();
        subscribeDefaultTopics();
    }

    /*
    Se anuncia ao iot cataloguer
     */
    private void sendKeepAlive() {

        val gateway = new Gateway();
        gateway.setDn(dn);
        gateway.setLat(120.0);
        gateway.setLon(120.0);
        gateway.setUrl(fogNodeUrl);

        postOnIotCataloguer(gateway);

    }

    /*
    Inicializa o cliente MQTT.
     */
    private void initMqtt() {
        val mqttOpts = new MqttConnectOptions();
        mqttOpts.setCleanSession(true);
        mqttOpts.setAutomaticReconnect(true);
        mqttOpts.setConnectionTimeout(30000);

        val tmpDir = System.getProperty("java.io.tmpdir");
        val dataStore = new MqttDefaultFilePersistence(tmpDir);

        try{
            mqttClient = new MqttClient(brokerUrl, dn, dataStore);
            mqttClient.connect(mqttOpts);

        } catch (MqttException e){
            e.printStackTrace();
        }
    }

    /*
    Subscreve nos tópicos padrão do CDPO para comunicação com os Edges
     */
    private void subscribeDefaultTopics() {
        try {
            mqttClient.subscribe(CDPO_EDGE_EVENT +"+/+", (topic, mqttMessage) -> {
                new Thread(() -> {
                    // recebeu um evento do edge. envia para o Cep processar
                    try {
                        val mapper = new ObjectMapper();
                        val event = mapper.readValue(mqttMessage.getPayload(), Map.class);
                        val eventType = (String) event.get("eventType");
                        cepService.send(event, eventType);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            });
            mqttClient.subscribe( CDPO_DEPLOY_STATUS + "+/+", (topic, mqttMessage) -> {
                new Thread(() -> {
                    // o topico tem o formato /cdpo/deploy-status/hostUuid/ruleUuid
                    val t = topic.split("/");

                    // TODO: substituir por objeto DeployedStatus
                    Map<String, Object> deployStatus  = new HashMap<>();
                    deployStatus.put("ruleUuid", t[3]);
                    deployStatus.put("hostUuid", t[2]);
                    deployStatus.put("status", new String(mqttMessage.getPayload()));

                    // envia o status do deploy para o cdpo
                    RestTemplate restTemplate = new RestTemplate();
                    // TODO: restTemplate.postForObject(cdpoUrl+EPN_DEPLOY_STATUS, deployStatus, Map.class);
                }).start();
            });
            mqttClient.subscribe(EDGE_KEEP_ALIVE_TOPIC, (topic, mqttMessage) -> {
                new Thread(() -> {
                    try {
                        val mapper = new ObjectMapper();
                        val resource = mapper.readValue(mqttMessage.getPayload(), Resource.class);
                        postOnIotCataloguer(resource);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /*
    Envia regras para um edge
     */
    public void sendRuleToEdge(Resource resource, List<Rule> rules) {

        for (val rule : rules) {
            try {
                val objectMapper = new ObjectMapper();
                val payload = objectMapper.writeValueAsBytes(rule);
                publish(CDPO_EDGE_RULE + resource.getUuid(), payload);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

    }

    /*
    Trata as regras processadas na fog
     */
    public void processInFog(Rule rule) {

        addEventTypes(rule);

        // se o resultado da regra deve ser enviada a fog...
        if (rule.getTarget().equals(Level.FOG)) {
            // insere os eventos com o nome da regra para que
            // o fog possa processar localmente
            val insertRule = "insert into " + rule.getName() + " " + rule.getDefinition();
            cepService.addRule(insertRule, rule.getName());
        }
        else if (rule.getTarget().equals(Level.CLOUD)) {
            // se o resultado da regra deve ser enviada à cloud ...
            // adiciona a regra no CepService e o listener associado
            // que envia os resultados para a forwardUrl (cloud)
            val stm = cepService.addRule(rule.getDefinition(), rule.getName());
            stm.addListener((eventBeans, eventBeans1) -> {
                new Thread(() -> {
                    val event = (Map) eventBeans[0].getUnderlying();
                    val restTemplate = new RestTemplate();
                    val topic = cdpoComposerUrl + CDPO_COMPOSER_PUBLISH_EVENT + rule.getUuid();
                    restTemplate.postForObject(topic, event, Map.class);
                }).start();
            });
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid Rule Target");
        }

    }

    /*
    Adiciona os event types ao serviço Cep
     */
    private void addEventTypes(Rule rule) {
        val eventTypes = rule.getEventTypes();
        eventTypes.forEach(eventType -> cepService.addEventType(eventType));
    }

    /*
    Envia uma requisição relate para o iotcataloguer.
    */
    private void postOnIotCataloguer(Resource resource) {

        val headers = new HttpHeaders();
        headers.set(HEADER_DN_KEY, dn);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        val entity = new HttpEntity<>(resource, headers);

        val restTemplate = new RestTemplate();
        restTemplate.postForObject(iotCataloguerUrl + IOT_CATALOGUER_GATEWAY_RELATE, entity, Resource.class);

    }

    /*
    Envia uma requisição gateway para o iotcataloguer.
    */
    private void postOnIotCataloguer(Gateway gateway) {

        val headers = new HttpHeaders();
        headers.set(HEADER_DN_KEY, dn);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        val entity = new HttpEntity<>(gateway, headers);

        val restTemplate = new RestTemplate();
        restTemplate.postForObject(iotCataloguerUrl + IOT_CATALOGUER_GATEWAY, entity, Gateway.class);

    }

    /*
    Publica uma mensagem no serviço MQTT
     */
    public void publish(String topic, byte[] payload) {
        try{
            val message = new MqttMessage();
            message.setQos(0);
            message.setPayload(payload);
            mqttClient.publish(topic, message);
        } catch (MqttException e){
            e.printStackTrace();
        }
    }

}
