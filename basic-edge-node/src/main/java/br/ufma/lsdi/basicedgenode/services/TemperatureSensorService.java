package br.ufma.lsdi.basicedgenode.services;

import com.espertech.esper.client.EPStatement;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

/*
Esta classe representa um sensor de temperatura.
 */
@Service
public class TemperatureSensorService {

    /*
    Gera uma temperatura em intervalos aleatórios (entre 0 e 5 segundos).
    A temperatura inicia com 30 e vai aumentando ou diminuindo a cada
    intervalo.
     */
    public TemperatureSensorService(CepService cepService) throws InterruptedException {

        cepService.addEventType("Temperature", new Properties());
        cepService.addEventType("TemperatureG", new Properties());

        Map props = new HashMap<>();
        props.put("value", Integer.class);
        cepService.addEventType("TemperatureH", props);

        // testa processamento de eventos localmente
        // 1 - processa temperatura (criada localmente)
        EPStatement stm = cepService.addRule("select * from Temperature", "Temperature");
        stm.addListener((eventBeans, eventBeans1) -> {
            Map temperature = (Map) eventBeans[0].getUnderlying();
            System.out.println(">>>" + temperature);
        });

        // 2 - processa temperatura h (criada por regra enviada pelo fog)
        EPStatement stm2 = cepService.addRule("select * from TemperatureH where value >= 35 or value <= 25", "TemperatureG");
        stm2.addListener((eventBeans, eventBeans1) -> {
            Map temperatureH = (Map) eventBeans[0].getUnderlying();
            System.out.println("+++++" + temperatureH);
        });


        // gera temperaturas e envia para o cep service

        int temperature = 30;
        int id = 1;


        while(true) {
            Thread.sleep(new Random().nextInt(6 * 1000));  // pausa aleatória de 0 a 5 segundos.
            int flag = new Random().nextInt(3); // 0 = aumento, 1 = diminuição, 2 = mantém valor
            int delta = new Random().nextInt(6); // valor a diminuir entre 0 e 5 graus
            switch (flag) {
                case 0:
                    temperature = temperature + delta;
                    break;
                case 1:
                    temperature = temperature - delta;
                    break;
            }

            // envia a temperatura para o serviço de Cep
            Map temp = new HashMap();
            temp.put("id", id);
            temp.put("value", temperature);
            cepService.send(temp, "Temperature");

            id++;

        }
    }

}
