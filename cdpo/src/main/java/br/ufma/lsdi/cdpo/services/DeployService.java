package br.ufma.lsdi.cdpo.services;

import br.ufma.lsdi.cdpo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/*
Classe de serviço com métodos para realizar o deploy da epn na rede.
 */
@Service
public class DeployService {

    @Value("${cdpo.iotcataloguer.url}")
    private String iotCataloguerUrl;

    public void deploy(Epn epn) {

        Map<String, Deploy> deployMap = new HashMap<>();

        epn.getRules().forEach(rule -> {

            if (rule.getLevel().equals(Level.EDGE)) {

            }
            else if (rule.getLevel().equals(Level.FOG)) {
                // acha os  gateways
                List<Gateway> gateways = findGateways(rule.getTagFilter());;
                // para cada gateway...
                gateways.forEach(gateway -> {
                    // cria um Deploy e adiciona a regra nele
                    if (!deployMap.containsKey(gateway.getUuid())) {
                        deployMap.put(gateway.getUuid(), new Deploy());
                        deployMap.get(gateway.getUuid()).setUuid(UUID.randomUUID().toString());
                        deployMap.get(gateway.getUuid()).setEpn(epn);
                        deployMap.get(gateway.getUuid()).setGateway(gateway);
                    }
                    if (deployMap.get(gateway.getUuid()).getRules() == null) {
                        deployMap.get(gateway.getUuid()).setRules(new ArrayList<>());
                    }
                    deployMap.get(gateway.getUuid()).getRules().add(rule);
                });
            }
            else if (rule.getLevel().equals(Level.CLOUD)) {

            }


        });

        System.out.println(deployMap);

    }

    // encontra todos os gateways de acordo com as tags
    private List<Gateway> findGateways(String tagFilter) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> request = new HttpEntity<>(tagFilter);
        ResponseEntity<List<Gateway>> response = restTemplate.exchange(iotCataloguerUrl + "iot-cataloguer/gateway/expression", HttpMethod.POST, request, new ParameterizedTypeReference<List<Gateway>>() {});
        List<Gateway> gateways = response.getBody();
        return gateways;
    }

}
