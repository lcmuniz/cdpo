package br.ufma.lsdi.basicfognode.services;

import lombok.val;
import org.springframework.stereotype.Service;

/*
Esta classe simula o processamento deste Fog Node
 */
@Service
public class BasicFogNodeService {

    private final CepService cepService;

    public BasicFogNodeService(CepService cepService) {
        this.cepService = cepService;

        new Thread(() -> {

            cepService.addEventType("TemperatureG");
            val stm = cepService.addRule("select * from TemperatureG");
            stm.addListener((eventBeans, eventBeans1) -> {
                new Thread(() -> System.out.println("### TemperaturaG - " + eventBeans[0].getUnderlying())).start();
            });

            cepService.addEventType("TemperatureH");
            val stm2 = cepService.addRule("select * from TemperatureH");
            stm2.addListener((eventBeans, eventBeans1) -> {
                new Thread(() -> System.out.println("### TemperaturaH - " + eventBeans[0].getUnderlying())).start();
            });

        }).start();

    }

}
