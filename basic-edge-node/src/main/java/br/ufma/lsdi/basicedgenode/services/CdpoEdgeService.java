package br.ufma.lsdi.basicedgenode.services;

import br.ufma.lsdi.cdpo.EventType;
import br.ufma.lsdi.cdpo.Level;
import br.ufma.lsdi.cdpo.Resource;
import br.ufma.lsdi.cdpo.Rule;
import com.espertech.esper.client.EPStatement;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

/*
Esta classe é responsável por toda a comunicação com o CDPO
 */
@Service
public class CdpoEdgeService {

    // MQTT topics
    private final String EDGE_KEEP_ALIVE_TOPIC = "/cdpo/edge/keep-alive";
    private final String CDPO_EDGE_RULE = "/cdpo/edge-rule/";
    private final String CDPO_EDGE_EVENT = "/cdpo/edge-event/";
    private final String CDPO_DEPLOY_STATUS = "/cdpo/deploy-status/";

    @Value("${cdpo.mqttbroker.url}")
    private String brokerUrl;

    @Value("${cdpo.edge.clientUuid}")
    private String clientUuid;

    @Value("${cdpo.edge.name}")
    private String edgeName;

    private final CepService cepService;
    private MqttClient mqttClient;

    private final Logger logger = LoggerFactory.getLogger(CdpoEdgeService.class);

    public CdpoEdgeService(CepService cepService) {
        this.cepService = cepService;
    }

    /*
    Inicializa o serviço MQTT
     */
    @PostConstruct
    public void post() {
        initMqtt();
        initKeepAliveTask();
    }

    /*
    Se anuncia ao iot cataloguer periodicamente
     */
    private void initKeepAliveTask() {

        Resource resource = new Resource();
        resource.setUuid(clientUuid);
        resource.setName(edgeName);
        resource.setLat(30.0);  // esses valores de lat e lon devem vir de um serviço de localização
        resource.setLon(20.0);

        ObjectMapper mapper = new ObjectMapper();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    byte[] payload = mapper.writeValueAsBytes(resource);
                    publish(EDGE_KEEP_ALIVE_TOPIC, payload);
                    logger.debug(String.format("%s published to %s.", resource, EDGE_KEEP_ALIVE_TOPIC));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 10 * 60 * 60 * 1000);
    }

    /*
    Inicializa o cliente MQTT.
    Subscreve-se no tópico cdpo/edge-rule para receber as regras do cdpo
     */
    private void initMqtt() {
        MqttConnectOptions mqttOpts = new MqttConnectOptions();
        mqttOpts.setCleanSession(true);
        mqttOpts.setAutomaticReconnect(true);
        mqttOpts.setConnectionTimeout(30000);

        String tmpDir = System.getProperty("java.io.tmpdir");
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);

        try{
            mqttClient = new MqttClient(brokerUrl, clientUuid, dataStore);
            mqttClient.connect(mqttOpts);

            // subscreve o tópico /cdepo/edge-rule para receber regras do cdpo
            String topic = CDPO_EDGE_RULE + clientUuid;
            mqttClient.subscribe(topic, (s, mqttMessage) -> {
                // o callback executa em uma thread separada para não bloquear a aplicação
                new Thread(() -> {
                    // testa se a mensagem é do tópico cdpo/edge-rule.
                    // tecnicamente não é necessário pois este é o unico tópico
                    // no qual esse serviço se inscreve (mas isso pode mudar no futuro).
                    if (topic.contains(CDPO_EDGE_RULE)) {
                        try {
                            // recebe a regra
                            String payload = new String(mqttMessage.getPayload());
                            ObjectMapper mapper = new ObjectMapper();
                            Rule rule = mapper.readValue(payload, Rule.class);

                            // adiciona à engine Cep
                            addEdgeRule(rule);

                            // publica o status do deploy para o cdpo
                            publish(CDPO_DEPLOY_STATUS + clientUuid + "/" + rule.getUuid(), "deployed".getBytes());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                }).start();
            });

        } catch (MqttException e){
            e.printStackTrace();
        }
    }

    /*
    Adciona uma EdgeRule recebida  do cdpo à engine Cep.
    Registra um listener para os eventos que devem ser enviados á fog
    O listener publica os eventos na fog
     */
    public void addEdgeRule(Rule rule) {

        addEventTypes(rule);

        if (rule.getTarget().equals(Level.EDGE)) {
            // ... inclui o insert antes da regra para gerar os novos eventos
            String insertEPL = "insert into " + rule.getName() + " " + rule.getDefinition();

            // adiciona a regra para processamento local
            cepService.addRule(insertEPL, rule.getName());
        }
        else {
            if (rule.getTarget().equals(Level.FOG)) {

                // adiciona a regra para processamento na fog (via o listener)
                EPStatement stm = cepService.addRule(rule.getDefinition(), rule.getName());

                // ... se subscreve no statement para receber os eventos gerados pela regra de insert...
                stm.addListener((eventBeans, eventBeans1) -> {
                    // o listener executa em uma thread separada para não bloquear a aplicação
                    new Thread(() -> {
                        Map object = (Map) eventBeans[0].getUnderlying();
                        object.put("eventType", rule.getName());
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            String topic = CDPO_EDGE_EVENT + clientUuid + "/" + rule.getUuid();
                            // envia os eventos do Cep para o cdpo via MQTT
                            publish(topic, mapper.writeValueAsBytes(object));
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }).start();
                });

            }
        }

    }

    /*
    Adciona os event types da regra.
     */
    private void addEventTypes(Rule rule) {
        List<EventType> eventTypes = rule.getEventTypes();
        eventTypes.forEach(eventType -> cepService.addEventType(eventType));
    }

    /*
    Publica uma mensagem no serviço MQTT
     */
    private void publish(String topic, byte[] payload) {
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
