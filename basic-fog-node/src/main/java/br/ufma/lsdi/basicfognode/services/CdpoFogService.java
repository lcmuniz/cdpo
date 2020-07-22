package br.ufma.lsdi.basicfognode.services;

import br.ufma.lsdi.cdpo.Deploy;
import br.ufma.lsdi.cdpo.Gateway;
import br.ufma.lsdi.cdpo.Resource;
import br.ufma.lsdi.cdpo.Rule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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

        Gateway gateway = new Gateway();
        gateway.setDn(dn);
        gateway.setLat(10.0);
        gateway.setLon(120.0);
        gateway.setUrl(fogNodeUrl);

        postOnIotCataloguer(gateway);

    }

    /*
    Inicializa o cliente MQTT.
     */
    private void initMqtt() {
        MqttConnectOptions mqttOpts = new MqttConnectOptions();
        mqttOpts.setCleanSession(true);
        mqttOpts.setAutomaticReconnect(true);
        mqttOpts.setConnectionTimeout(30000);

        String tmpDir = System.getProperty("java.io.tmpdir");
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);

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
                        ObjectMapper mapper = new ObjectMapper();
                        Map event = mapper.readValue(mqttMessage.getPayload(), Map.class);
                        String eventType = (String) event.get("eventType");
                        cepService.send(event, eventType);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            });
            mqttClient.subscribe( CDPO_DEPLOY_STATUS + "+/+", (topic, mqttMessage) -> {
                new Thread(() -> {
                    // o topico tem o formato /cdpo/deploy-status/hostUuid/ruleUuid
                    String[] t = topic.split("/");

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
                        ObjectMapper mapper = new ObjectMapper();
                        Resource resource = mapper.readValue(mqttMessage.getPayload(), Resource.class);
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
    Envia uma regra para a edge
     */
    public void sendRuleToEdge(Rule rule) {

        List<Deploy> deploys = rule.getDeploys();

        for (Deploy deploy : deploys) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                byte[] payload = objectMapper.writeValueAsBytes(rule);
                publish(CDPO_EDGE_RULE + deploy.getHostUuuid(), payload);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

    }

    /*
    Envia uma requisição relate para o iotcataloguer.
    */
    private void postOnIotCataloguer(Resource resource) {

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_DN_KEY, dn);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Resource> entity = new HttpEntity<>(resource, headers);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForObject(iotCataloguerUrl + IOT_CATALOGUER_GATEWAY_RELATE, entity, Resource.class);

    }

    /*
    Envia uma requisição gateway para o iotcataloguer.
    */
    private void postOnIotCataloguer(Gateway gateway) {

        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_DN_KEY, dn);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Gateway> entity = new HttpEntity<>(gateway, headers);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForObject(iotCataloguerUrl + IOT_CATALOGUER_GATEWAY, entity, Gateway.class);

    }

    /*
    Publica uma mensagem no serviço MQTT
     */
    public void publish(String topic, byte[] payload) {
        try{
            MqttMessage message = new MqttMessage();
            message.setQos(0);
            message.setPayload(payload);
            mqttClient.publish(topic, message);
        } catch (MqttException e){
            e.printStackTrace();
        }
    }

}
