package br.ufma.lsdi.cdpo.services;

import br.ufma.lsdi.cdpo.entities.*;
import br.ufma.lsdi.cdpo.repos.DeployRepository;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/*
Classe de serviço com métodos para realizar o deploy da epn na rede.
 */
@Service
public class DeployService {

    @Value("${cdpo.iotcataloguer.url}")
    private String iotCataloguerUrl;

    @Value("${cdpo.composer.url}")
    String cdpoComposerUrl;

    private DeployRepository deployRepository;

    RestTemplate restTemplate = new RestTemplate();

    Logger logger = LoggerFactory.getLogger(DeployService.class);

    public DeployService(DeployRepository deployRepository) {
        this.deployRepository = deployRepository;
    }

    /*
    Faz o deploy de uma EPN para os fog e edge nodes.
     */
    public void deploy(Epn epn) {

            // lista de deploys para fogs
            Map<String, Deploy> fogDeploys = new HashMap<>();
            // lista de deploys para edges
            Map<String, Deploy> edgeDeploys = new HashMap<>();

            // Gera uuid quando não existir
            epn.getRules().stream()
                .filter(rule -> rule.getUuid()==null)
                .forEach(rule -> rule.setUuid(UUID.randomUUID().toString()));

            // Map com as regras da fog name -> uuid
            Map<String, String> fogRuleUuid =
                    epn.getRules().stream()
                                .filter(rule -> rule.getLevel().equals(Level.FOG))
                                .collect(Collectors.toMap(Rule::getName,Rule::getUuid));
            // para cada regra...
            epn.getRules().forEach(rule -> {

                // seta o uuid da regra
 //               if (rule.getUuid() == null) rule.setUuid(UUID.randomUUID().toString());

                if (rule.getLevel().equals(Level.EDGE)) {
                    // acha os edges
                    val resources = findResources(rule.getTagFilter());

                    // para cada edge ...
                    resources.forEach(resource -> {
                        // pega o ultimo gateway
                        val gateway = resource.getLastGateway();
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
                } else if (rule.getLevel().equals(Level.FOG)) {
                    // acha os  gateways
                    val gateways = findGateways(rule.getTagFilter());

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
                } else if (rule.getLevel().equals(Level.CLOUD)) {
                    val cloudInputUuids = registerEventTypesOnCloud(rule, fogRuleUuid);

                    val cloudRule = new HashMap<String, Object>();
                    cloudRule.put("uuid", rule.getUuid());
                    cloudRule.put("name", rule.getName());
                    cloudRule.put("definition", rule.getDefinition());
                    cloudRule.put("description", rule.getDescription());
                    cloudRule.put("level", rule.getLevel());
                    cloudRule.put("qos", rule.getQos().toString());
                    cloudRule.put("tagfilter", rule.getTagFilter());
                    cloudRule.put("target", rule.getTarget());
                    cloudRule.put("inputs", cloudInputUuids);

                    String url = cdpoComposerUrl + "/cdpo/registerNewCdpoRules";
                    restTemplate.postForObject(url, cloudRule, Map.class);
                }

            });

            deploy2FogNodes(fogDeploys);
            deploy2EdgeNodes(edgeDeploys);

    }

    // Informa a Cloud que Tipos de Eventos esperar da Fog
    private List<String> registerEventTypesOnCloud(Rule rule, Map<String, String> fogRuleUuid) {
        List<String> uuids = new ArrayList<>();

        rule.getEventTypes().forEach(eventType -> {
            //TODO evitar registro duplicado
            if(fogRuleUuid.containsKey(eventType.getName())) {
                eventType.setUuid(fogRuleUuid.get(eventType.getName()));
                Map<String, Object> inputSpec = new HashMap<>();
                inputSpec.put("uuid", eventType.getUuid());
                inputSpec.put("name", eventType.getName());
                inputSpec.put("eventSpec", eventType.getAttributes().stream()
                                                    .collect(Collectors.toMap(EventTypeAttribute::getName, EventTypeAttribute::getType))
                );

                String url = cdpoComposerUrl + "/cdpo/registerNewCdpoEvent";
                restTemplate.postForObject(url, inputSpec, Map.class);

                uuids.add(eventType.getUuid());
            }
            else
                logger.warn("EventType {} foi informado para regra {} mas não existe regra que o gere", eventType.getName(), rule.getName());

        });
        return  uuids;
    }

    // faz o deloy enviando para cada gateway suas regras
    private void deploy2FogNodes(Map<String, Deploy> fogDeploys) {
        fogDeploys.forEach((uuids, deploy) -> {

            // antes de salvar, insere os uuids da epn em cada regra
            deploy.getRules().stream().forEach(r -> r.setEpn(deploy.getEpn()));

            deployRepository.save(deploy);

            // após salvar, remove para que a resposta json não tenha o campo
            // e não cause stack overflow por referência circular
            deploy.getRules().stream().forEach(r -> r.setEpn(null));

            val restTemplate = new RestTemplate();
            val request = new HttpEntity<>(deploy);
            restTemplate.exchange(deploy.getGateway().getUrl() + "/deploy-fog", HttpMethod.POST, request, new ParameterizedTypeReference<Deploy>() {});
        });
    }

    // faz o deloy enviando para cada gateway as regras de seus edges
    private void deploy2EdgeNodes(Map<String, Deploy> edgeDeploys) {
        edgeDeploys.forEach((uuids, deploy) -> {

            // antes de salvar, insere os uuids da epn em cada regra
            deploy.getRules().stream().forEach(r -> r.setEpn(deploy.getEpn()));

            deployRepository.save(deploy);

            // após salvar, remove para que a resposta json não tenha o campo
            // e não cause stack overflow por referência circular
            deploy.getRules().stream().forEach(r -> r.setEpn(null));

            val restTemplate = new RestTemplate();
            val request = new HttpEntity<>(deploy);
            restTemplate.exchange(deploy.getGateway().getUrl() + "/deploy-edge", HttpMethod.POST, request, new ParameterizedTypeReference<Deploy>() {});
        });
    }

    // encontra todos os gateways de acordo com as tags
    private List<Gateway> findGateways(String tagFilter) {
        val restTemplate = new RestTemplate();
        val request = new HttpEntity<>(tagFilter);
        val response = restTemplate.exchange(iotCataloguerUrl + "iot-cataloguer/gateway/expression", HttpMethod.POST, request, new ParameterizedTypeReference<List<Gateway>>() {});
        return response.getBody();
    }

    // encontra todos os resources (edges) de acordo com as tags
    private List<Resource> findResources(String tagFilter) {
        val restTemplate = new RestTemplate();
        val request = new HttpEntity<>(tagFilter);
        val response = restTemplate.exchange(iotCataloguerUrl + "iot-cataloguer/resource/expression", HttpMethod.POST, request, new ParameterizedTypeReference<List<Resource>>() {});
        return response.getBody();
    }

}
