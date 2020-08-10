package br.ufma.lsdi.combededgenode.services;

import br.ufma.lsdi.combededgenode.models.Power;
import lombok.val;
import lombok.var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.*;

/*
Esta classe representa um sensor de temperatura.
 */
@Service
public class PowerSensorService {
    @Value("${combed.power.file}")
    String filePath;
    @Autowired
    Environment environment;

    /*
    Gera uma temperatura em intervalos aleatórios (entre 0 e 5 segundos).
    A temperatura inicia com 30 e vai aumentando ou diminuindo a cada
    intervalo.
     */
    public PowerSensorService(CepService cepService) throws InterruptedException {

        /*cepService.addEventType("Temperature");
        cepService.addEventType("TemperatureG");

        val map = new HashMap<>();
        map.put("value", Integer.class);
        cepService.addEventType("TemperatureH", map);

        // testa processamento de eventos localmente
        // 1 - processa temperatura (criada localmente)
        val stm = cepService.addRule("select * from Temperature", "Temperature");
        stm.addListener((eventBeans, eventBeans1) -> {
            val temperature = (Map) eventBeans[0].getUnderlying();
            System.out.println(">>> Temperature - " + temperature);
        });

        // 2 - processa temperatura h (criada por regra enviada pelo fog)
        val stm2 = cepService.addRule("select * from TemperatureH where value >= 35 or value <= 25", "TemperatureG");
        stm2.addListener((eventBeans, eventBeans1) -> {
            val temperatureH = (Map) eventBeans[0].getUnderlying();
            System.out.println(">>> TemperatureH - " + temperatureH);
        });

        // gera temperaturas e envia para o cep service

        //var temperature = 30;
        //var id = 1;
*/

    }
}
