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

    /*
    Faz o deploy de uma EPN para os fog e edge nodes.
     */
    public void deploy(Epn epn) {

        // lista de deploys para fogs
        Map<String, Deploy> deployFogRules = new HashMap<>();
        // lista de deploys para edges
        Map<String, Deploy> deployEdgeRules = new HashMap<>();

        // para cada regra...
        epn.getRules().forEach(rule -> {

            // seta o uuid da regra
            if (rule.getUuid() == null) rule.setUuid(UUID.randomUUID().toString());

            if (rule.getLevel().equals(Level.EDGE)) {
                // acha os edges
                List<Resource> resources = findResources(rule.getTagFilter());;
                // para cada edge ...
                resources.forEach(resource -> {
                    // pega o ultimo gateway
                    Gateway gateway = resource.getLastGateway();
                    if (!deployEdgeRules.containsKey(gateway.getUuid())) {
                        // cria um deploy para este gateway
                        deployEdgeRules.put(gateway.getUuid(), new Deploy());
                        deployEdgeRules.get(gateway.getUuid()).setUuid(UUID.randomUUID().toString());
                        deployEdgeRules.get(gateway.getUuid()).setEpn(epn);
                        deployEdgeRules.get(gateway.getUuid()).setGateway(gateway);
                    }
                    // adiciona o edge ao deploy
                    if (deployEdgeRules.get(gateway.getUuid()).getResources() == null) {
                        deployEdgeRules.get(gateway.getUuid()).setResources(new ArrayList<>());
                    }
                    if (!deployEdgeRules.get(gateway.getUuid()).getResources().contains(resource)) {
                        deployEdgeRules.get(gateway.getUuid()).getResources().add(resource);
                    }
                    // adiciona a regra ao deploy
                    if (deployEdgeRules.get(gateway.getUuid()).getRules() == null) {
                        deployEdgeRules.get(gateway.getUuid()).setRules(new ArrayList<>());
                    }
                    if (!deployEdgeRules.get(gateway.getUuid()).getRules().contains(rule)) {
                        deployEdgeRules.get(gateway.getUuid()).getRules().add(rule);
                    }
                });
            }
            else if (rule.getLevel().equals(Level.FOG)) {
                // acha os  gateways
                List<Gateway> gateways = findGateways(rule.getTagFilter());;
                // para cada gateway...
                gateways.forEach(gateway -> {
                    if (!deployFogRules.containsKey(gateway.getUuid())) {
                        // cria um deploy para este gateway
                        deployFogRules.put(gateway.getUuid(), new Deploy());
                        deployFogRules.get(gateway.getUuid()).setUuid(UUID.randomUUID().toString());
                        deployFogRules.get(gateway.getUuid()).setEpn(epn);
                        deployFogRules.get(gateway.getUuid()).setGateway(gateway);
                    }
                    // adiciona a regra ao deploy
                    if (deployFogRules.get(gateway.getUuid()).getRules() == null) {
                        deployFogRules.get(gateway.getUuid()).setRules(new ArrayList<>());
                    }
                    deployFogRules.get(gateway.getUuid()).getRules().add(rule);
                });
            }
            else if (rule.getLevel().equals(Level.CLOUD)) {

            }

        });

        System.out.println("DEPLOYS PARA OS FOG NODES: " + deployFogRules);
        System.out.println("DEPLOYS PARA OS EDGE NODES:" + deployEdgeRules);

    }

    // encontra todos os gateways de acordo com as tags
    private List<Gateway> findGateways(String tagFilter) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> request = new HttpEntity<>(tagFilter);
        ResponseEntity<List<Gateway>> response = restTemplate.exchange(iotCataloguerUrl + "iot-cataloguer/gateway/expression", HttpMethod.POST, request, new ParameterizedTypeReference<List<Gateway>>() {});
        List<Gateway> gateways = response.getBody();
        return gateways;
    }

    // encontra todos os resources (edges) de acordo com as tags
    private List<Resource> findResources(String tagFilter) {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> request = new HttpEntity<>(tagFilter);
        ResponseEntity<List<Resource>> response = restTemplate.exchange(iotCataloguerUrl + "iot-cataloguer/resource/expression", HttpMethod.POST, request, new ParameterizedTypeReference<List<Resource>>() {});
        List<Resource> resources = response.getBody();
        return resources;
    }

}
