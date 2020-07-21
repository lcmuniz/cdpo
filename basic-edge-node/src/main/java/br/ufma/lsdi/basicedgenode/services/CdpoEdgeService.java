package br.ufma.lsdi.basicedgenode.services;

import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
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

        Map map = new HashMap<>();
        map.put("uuid", clientUuid);
        map.put("name", edgeName);
        map.put("lat", 10.0);  // esses valores de lat e lon devem vir de um serviço de localização
        map.put("lon", 20.0);

        ObjectMapper mapper = new ObjectMapper();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    byte[] payload = mapper.writeValueAsBytes(map);
                    publish(EDGE_KEEP_ALIVE_TOPIC, payload);
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
                            Map map = mapper.readValue(payload, Map.class);

                            // adiciona à engine Cep
                            addEdgeRule(map);

                            // publica o status do deploy para o cdpo
                            publish(CDPO_DEPLOY_STATUS + clientUuid + "/" + map.get("uuid"), "deployed".getBytes());
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
    public void addEdgeRule(Map<String, Object> rule) {

        String target = (String) rule.get("target");
        String definition = (String) rule.get("definition");
        String name = (String) rule.get("name");
        String ruuid = (String) rule.get("uuid");

        addEventTypes(rule);


        if (target.equals("edge")) {
            // ... inclui o insert antes da regra para gerar os novos eventos
            String insertEPL = "insert into " + name + " " + definition;

            // adiciona a regra
            cepService.addRule(insertEPL, name);
        }
        else {
            if (target.equals("fog")) {

                // adiciona a regra
                EPStatement stm = cepService.addRule(definition, name);

                // ... se subscreve no statement para receber os eventos gerados pela regra de insert...
                stm.addListener((eventBeans, eventBeans1) -> {
                    // o listener executa em uma thread separada para não bloquear a aplicação
                    new Thread(() -> {
                        Map object = (Map) eventBeans[0].getUnderlying();
                        object.put("eventType", name);
                        try {
                            ObjectMapper mapper = new ObjectMapper();
                            String topic = CDPO_EDGE_EVENT + clientUuid + "/" + ruuid;
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

    private void addEventTypes(Map<String, Object> rule) {
        List<Map<String, Object>> eventTypes = (List) rule.get("eventTypes");
        eventTypes.forEach(eventType -> {

            String eventTypeName = (String) eventType.get("name");

            List<Map<String, String>> attributes = (List) eventType.get("attributes");
            Map map = new HashMap();
            if (attributes != null) {
                attributes.forEach(attribute -> {
                    try {
                        Class clazz = Class.forName(attribute.get("type"));
                        map.put(attribute.get("name"), clazz);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                });
            }

            cepService.addEventType(eventTypeName, map);

        });
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
