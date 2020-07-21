package br.ufma.lsdi.basicedgenode.services;

import br.ufma.lsdi.basicedgenode.models.Temperature;
import com.espertech.esper.client.EPStatement;
import org.springframework.stereotype.Service;

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

        cepService.addEventType("Temperature", Temperature.class);

        Properties props = new Properties();
        props.setProperty("id", "java.lang.Integer");
        props.setProperty("value", "java.lang.Integer");
        cepService.addEventType("TemperatureH", props);

        EPStatement stm = cepService.addRule("select * from Temperature");
        stm.addListener((eventBeans, eventBeans1) -> {
            Temperature temperature = (Temperature) eventBeans[0].getUnderlying();
            System.out.println(">>>" + temperature);
        });

        EPStatement stm2 = cepService.addRule("select * from TemperatureH where value >= 35 or value <= 25");
        stm2.addListener((eventBeans, eventBeans1) -> {
            Map temperatureH = (Map) eventBeans[0].getUnderlying();
            System.out.println("+++++" + temperatureH);
        });

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
            cepService.send(new Temperature(id, temperature));

            id++;

        }
    }

}
