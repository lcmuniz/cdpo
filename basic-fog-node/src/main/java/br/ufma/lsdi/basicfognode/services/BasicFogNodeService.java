package br.ufma.lsdi.basicfognode.services;

import com.espertech.esper.client.EPStatement;
import org.springframework.stereotype.Service;

import java.util.Properties;

/*
Esta classe simula o processamento deste Fog Node
 */
@Service
public class BasicFogNodeService {

    private final CepService cepService;

    public BasicFogNodeService(CepService cepService) {
        this.cepService = cepService;

        new Thread(() -> {

            cepService.addEventType("TemperatureH", new Properties());
            EPStatement stm = cepService.addRule("select * from TemperatureH");
            stm.addListener((eventBeans, eventBeans1) -> {
                new Thread(() -> System.out.println(">>>>>>>>>>FOG" + eventBeans[0].getUnderlying())).start();
            });

            cepService.addEventType("TemperatureX", new Properties());
            EPStatement stm2 = cepService.addRule("select * from TemperatureX", "TemperatureX");
            stm2.addListener((eventBeans, eventBeans1) -> {
                new Thread(() -> System.out.println(">>>>>>>>>>FOG222222222222222" + eventBeans[0].getUnderlying())).start();
            });

        }).start();

    }

}
