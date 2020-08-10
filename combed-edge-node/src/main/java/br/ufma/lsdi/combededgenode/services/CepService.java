package br.ufma.lsdi.combededgenode.services;

import br.ufma.lsdi.combededgenode.models.EventType;
import br.ufma.lsdi.combededgenode.models.Power;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import lombok.val;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/*
Serviço de CEP
 */
@Service
public class CepService {

    private EPServiceProvider engine = EPServiceProviderManager.getDefaultProvider();

    /*
    Adciona um tipo de evento para ser reconhecido pelo engine Cep
     */
    public void addEventType(String eventType, Class clazz) {
        engine.getEPAdministrator().getConfiguration().addEventType(eventType, clazz);
    }

    /*
    Adciona um tipo de evento para ser reconhecido pelo engine Cep
     */
    public void addEventType(String eventTypeName) {
        val et = new EventType();
        et.setName(eventTypeName);
        et.setAttributes(null);
        addEventType(et);
    }

    /*
    Adciona um tipo de evento e seus atributos para ser reconhecido pelo engine Cep
     */
    public void addEventType(EventType eventType) {

        // coloca todos as atributos em um map
        // para passar para o engine cep
        val map = new HashMap();
        if (eventType.getAttributes() != null) {
            eventType.getAttributes().forEach(attribute -> {
                try {
                    Class clazz = Class.forName(attribute.getType());
                    map.put(attribute.getName(), clazz);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
        }

        addEventType(eventType.getName(), map);

    }

    /*
    Adciona um tipo de evento e seus atributos para ser reconhecido pelo engine Cep
     */
    public void addEventType(String eventTypeName, Map attributesMap) {

        val et = engine.getEPAdministrator().getConfiguration().getEventType(eventTypeName);
        if (et == null) {
            engine.getEPAdministrator().getConfiguration().addEventType(eventTypeName, attributesMap);
        }
        else {
            engine.getEPAdministrator().getConfiguration().updateMapEventType(eventTypeName, attributesMap);
        }
    }


    /*
    Adciona uma regra para ser processada pelo engine Cep.
    Retorna o statement.
     */
    public EPStatement addRule(String definition, String eventType) {
        return engine.getEPAdministrator().createEPL(definition, eventType);
    }

    /*
    Envia um evento para a engine Cep.
     */
    public void send(Map object, String eventType) {
        engine.getEPRuntime().sendEvent(object, eventType);
    }
}
