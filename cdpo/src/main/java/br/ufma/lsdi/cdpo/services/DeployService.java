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
        Map<String, Deploy> fogDeploys = new HashMap<>();
        // lista de deploys para edges
        Map<String, Deploy> edgeDeploys = new HashMap<>();

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
                    if (!edgeDeploys.containsKey(gateway.getUuid())) {
                        // cria um deploy para este gateway
                        edgeDeploys.put(gateway.getUuid(), new Deploy());
                        edgeDeploys.get(gateway.getUuid()).setUuid(UUID.randomUUID().toString());
                        edgeDeploys.get(gateway.getUuid()).setEpn(epn);
                        edgeDeploys.get(gateway.getUuid()).setGateway(gateway);
                    }
                    // adiciona o edge ao deploy
                    if (edgeDeploys.get(gateway.getUuid()).getResources() == null) {
                        edgeDeploys.get(gateway.getUuid()).setResources(new ArrayList<>());
                    }
                    if (!edgeDeploys.get(gateway.getUuid()).getResources().contains(resource)) {
                        edgeDeploys.get(gateway.getUuid()).getResources().add(resource);
                    }
                    // adiciona a regra ao deploy
                    if (edgeDeploys.get(gateway.getUuid()).getRules() == null) {
                        edgeDeploys.get(gateway.getUuid()).setRules(new ArrayList<>());
                    }
                    if (!edgeDeploys.get(gateway.getUuid()).getRules().contains(rule)) {
                        edgeDeploys.get(gateway.getUuid()).getRules().add(rule);
                    }
                });
            }
            else if (rule.getLevel().equals(Level.FOG)) {
                // acha os  gateways
                List<Gateway> gateways = findGateways(rule.getTagFilter());;
                // para cada gateway...
                gateways.forEach(gateway -> {
                    if (!fogDeploys.containsKey(gateway.getUuid())) {
                        // cria um deploy para este gateway
                        fogDeploys.put(gateway.getUuid(), new Deploy());
                        fogDeploys.get(gateway.getUuid()).setUuid(UUID.randomUUID().toString());
                        fogDeploys.get(gateway.getUuid()).setEpn(epn);
                        fogDeploys.get(gateway.getUuid()).setGateway(gateway);
                    }
                    // adiciona a regra ao deploy
                    if (fogDeploys.get(gateway.getUuid()).getRules() == null) {
                        fogDeploys.get(gateway.getUuid()).setRules(new ArrayList<>());
                    }
                    fogDeploys.get(gateway.getUuid()).getRules().add(rule);
                });
            }
            else if (rule.getLevel().equals(Level.CLOUD)) {

            }

        });

        deploy2FogNodes(fogDeploys);
        deploy2EdgeNodes(fogDeploys);

    }

    // faz o deloy enviando para cada gateway suas regras
    private void deploy2FogNodes(Map<String, Deploy> fogDeploys) {
        fogDeploys.forEach((uuids, deploy) -> {
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<Deploy> request = new HttpEntity<>(deploy);
            restTemplate.exchange(deploy.getGateway().getUrl() + "/deploy-fog", HttpMethod.POST, request, new ParameterizedTypeReference<Deploy>() {});
        });
    }

    // faz o deloy enviando para cada gateway as regras de seus edges
    private void deploy2EdgeNodes(Map<String, Deploy> edgeDeploys) {
        edgeDeploys.forEach((uuids, deploy) -> {
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<Deploy> request = new HttpEntity<>(deploy);
            restTemplate.exchange(deploy.getGateway().getUrl() + "/deploy-edge", HttpMethod.POST, request, new ParameterizedTypeReference<Deploy>() {});
        });
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
