package br.ufma.lsdi.basicedgenode.services;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import org.springframework.stereotype.Service;

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
    Adciona um tipo de evento para ser reconhecido pelo engine Cep
     */
    public void addEventType(String eventType, Properties properties) {
        engine.getEPAdministrator().getConfiguration().addEventType(eventType, properties);
    }

    /*
    Adciona uma regra para ser processada pelo engine Cep.
    Retorna o statement.
     */
    public EPStatement addRule(String definition) {
        return engine.getEPAdministrator().createEPL(definition);
    }

    /*
    Envia um evento para a engine Cep.
     */
    public void send(Object object) {
        engine.getEPRuntime().sendEvent(object);
    }
}
