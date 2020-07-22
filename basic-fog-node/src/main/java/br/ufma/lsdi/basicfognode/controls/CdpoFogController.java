package br.ufma.lsdi.basicfognode.controls;

import br.ufma.lsdi.basicfognode.services.CdpoFogService;
import br.ufma.lsdi.basicfognode.services.CepService;
import br.ufma.lsdi.cdpo.EventType;
import br.ufma.lsdi.cdpo.EventTypeAttribute;
import br.ufma.lsdi.cdpo.Level;
import br.ufma.lsdi.cdpo.Rule;
import com.espertech.esper.client.EPStatement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
Esta classe representa o serviço rest do fog node.
Ela é responsável por receber as regras a serem processadas
pelo fog nodes e seus edge nodes associados.
 */
@RestController
public class CdpoFogController {

    @Value("${cdpo.composer.url}")
    private String cdpoComposerUrl;

    private final String CDPO_COMPOSER_PUBLISH_EVENT = "/cdpo/publishNewCdpoEvent/";

    private final CepService cepService;
    private final CdpoFogService cdpoFogService;

    public CdpoFogController(CepService cepService, CdpoFogService cdpoFogService) {
        this.cepService = cepService;
        this.cdpoFogService = cdpoFogService;
    }
    /*
    Adiciona uma regra ao serviço de Cep para processamento.
     */
    @PostMapping("rule")
    public Rule addRule(@RequestBody Rule rule) {

        if (rule.getLevel().equals(Level.EDGE)) {
            cdpoFogService.sendRuleToEdge(rule);
        }
        else if (rule.getLevel().equals(Level.FOG)) {
            processInFog(rule);
        }
        else if (rule.getLevel().equals(Level.CLOUD)) {
            sendToCloud(rule);
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid Rule Level");
        }

        return rule;
    }

    /*
    Trata as regras que devem ser processadas na cloud
     */
    private void sendToCloud(Rule rule) {

    }

    /*
    Trata as regras processadas na fog
     */
    private void processInFog(Rule rule) {

        addEventTypes(rule);

        // se o resultado da regra deve ser enviada a fog...
        if (rule.getTarget().equals(Level.FOG)) {
            // insere os eventos com o nome da regra para que
            // o fog possa processar localmente
            String insertRule = "insert into " + rule.getName() + " " + rule.getDefinition();
            cepService.addRule(insertRule, rule.getName());
        }
        else if (rule.getTarget().equals(Level.CLOUD)) {
            // se o resultado da regra deve ser enviada à cloud ...
            // adiciona a regra no CepService e o listener associado
            // que envia os resultados para a forwardUrl (cloud)
            EPStatement stm = cepService.addRule(rule.getDefinition(), rule.getName());
            stm.addListener((eventBeans, eventBeans1) -> {
                new Thread(() -> {
                    Map event = (Map) eventBeans[0].getUnderlying();
                    RestTemplate restTemplate = new RestTemplate();
                    String topic = cdpoComposerUrl + CDPO_COMPOSER_PUBLISH_EVENT + rule.getUuid();
                    restTemplate.postForObject(topic, event, Map.class);
                }).start();
            });
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid Rule Target");
        }

    }

    /*
    Adiciona os event types ao serviço Cep
     */
    private void addEventTypes(Rule rule) {
        List<EventType> eventTypes = rule.getEventTypes();
        eventTypes.forEach(eventType -> cepService.addEventType(eventType));
    }

}
