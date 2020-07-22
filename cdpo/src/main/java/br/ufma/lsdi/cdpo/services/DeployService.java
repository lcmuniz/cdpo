package br.ufma.lsdi.cdpo.services;

import br.ufma.lsdi.cdpo.Deploy;
import br.ufma.lsdi.cdpo.ObjectType;
import br.ufma.lsdi.cdpo.TaggedObject;
import br.ufma.lsdi.cdpo.TaggedObjectFilter;
import br.ufma.lsdi.cdpo.models.Epn;
import br.ufma.lsdi.cdpo.models.Level;
import br.ufma.lsdi.cdpo.models.Rule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/*
Classe de serviço com métodos para realizar o deploy da epn na rede.
 */
@Service
public class DeployService {

    @Value("${cdpo.tagger.url}")
    private String taggerUrl;

    public void deploy(Epn epn) {

        epn.getRules().stream().forEach(rule -> {

            if (rule.getLevel().equals(Level.EDGE)) {
                List<Deploy> deploys = getDeploys(rule, "EdgeNode");
                rule.setDeploys(deploys);

            }
            else if (rule.getLevel().equals(Level.FOG)) {
                List<Deploy> deploys = getDeploys(rule, "FogNode");
                rule.setDeploys(deploys);
            }
            else if (rule.getLevel().equals(Level.CLOUD)) {
                List<Deploy> deploys = getDeploys(rule, "Resource");
                rule.setDeploys(deploys);
            }

            //para fazer o deploy eh necessario o gateway de cada edge

            System.out.println(rule);

        });

    }

    private List<Deploy> getDeploys(Rule rule, String resourceType) {

        ObjectType ot = new ObjectType();
        ot.setType(resourceType);
        TaggedObjectFilter filter = new TaggedObjectFilter();
        filter.setObjectType(ot);
        filter.setExpression(rule.getTagFilter());

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<TaggedObjectFilter> entity = new HttpEntity<>(filter);
        List<TaggedObject> resources = restTemplate.postForObject(taggerUrl + "/tagger/tagged-object/tag-expression", entity, List.class);

        List<Deploy> deploys = resources.stream().map(resource -> {
            Deploy deploy = new Deploy();
            deploy.setUuid(UUID.randomUUID().toString());
            deploy.setHostUuuid(resource.getUuid());
            return deploy;
        }).collect(Collectors.toList());

        return deploys;

    }


}
