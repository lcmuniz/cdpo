package br.ufma.lsdi.basicedgenode.services;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventType;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Properties;

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
    Adciona um tipo de evento e seus atributos para ser reconhecido pelo engine Cep
     */
    public void addEventType(String eventType, Map properties) {
        EventType et = engine.getEPAdministrator().getConfiguration().getEventType(eventType);
        if (et == null) {
            engine.getEPAdministrator().getConfiguration().addEventType(eventType, properties);
        }
        else {
            engine.getEPAdministrator().getConfiguration().updateMapEventType(eventType, properties);
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
