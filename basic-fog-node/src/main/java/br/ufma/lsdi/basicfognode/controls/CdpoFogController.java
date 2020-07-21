package br.ufma.lsdi.basicfognode.controls;

import br.ufma.lsdi.basicfognode.services.CdpoFogService;
import br.ufma.lsdi.basicfognode.services.CepService;
import com.espertech.esper.client.EPStatement;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@RestController
public class CdpoFogController {

    private final CepService cepService;
    private final CdpoFogService cdpoFogService;

    public CdpoFogController(CepService cepService, CdpoFogService cdpoFogService) {
        this.cepService = cepService;
        this.cdpoFogService = cdpoFogService;
    }

    @PostMapping("rule")
    public Map addRule(@RequestBody Map<String, Object> rule) {

        String level = (String) rule.get("level");

        if (level.equals("edge")) {
            cdpoFogService.sendRuleToEdge(rule);
        }
        else if (level.equals("fog")) {
            processInFog(rule);
        }
        else if (level.equals("cloud")) {
            sendToCloud(rule);
        }

        return rule;
    }

    private void sendToCloud(Map<String, Object> rule) {

    }

    private void processInFog(Map<String, Object> rule) {
        String definition = (String) rule.get("definition");
        String name = (String) rule.get("name");
        String forwardUrl = (String) rule.get("forwardUrl");
        String target = (String) rule.get("target");
        List<Map<String, Object>> eventTypes = (List) rule.get("eventTypes");

        addEventTypes(rule);

        if (target.equals("fog")) {
            // insere os eventos com o nome da regra para que
            // o fog possa processar localmente
            String insertRule = "insert into " + name + " " + rule.get("definition");
            cepService.addRule(insertRule, name);
        }
        else {
            // adiciona a regra no CepService e o listener associado
            // que envia os resultados para a forwardUrl (cloud)
            EPStatement stm = cepService.addRule(definition, name);
            if (forwardUrl != null) {
                stm.addListener((eventBeans, eventBeans1) -> {
                    new Thread(() -> {
                        Object event = eventBeans[0].getUnderlying();
                        RestTemplate restTemplate = new RestTemplate();
                        restTemplate.postForObject(forwardUrl, event, Object.class);
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

}
