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

            cepService.addEventType("TemperatureH");
            EPStatement stm = cepService.addRule("select * from TemperatureH");
            stm.addListener((eventBeans, eventBeans1) -> {
                new Thread(() -> System.out.println("### TemperaturaH - " + eventBeans[0].getUnderlying())).start();
            });

            cepService.addEventType("TemperatureX");
            EPStatement stm2 = cepService.addRule("select * from TemperatureX", "TemperatureX");
            stm2.addListener((eventBeans, eventBeans1) -> {
                new Thread(() -> System.out.println("### TemperaturaX - " + eventBeans[0].getUnderlying())).start();
            });

        }).start();

    }

}
