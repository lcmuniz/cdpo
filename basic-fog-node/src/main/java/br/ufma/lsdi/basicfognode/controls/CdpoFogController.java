package br.ufma.lsdi.basicfognode.controls;

import br.ufma.lsdi.basicfognode.services.CepService;
import com.espertech.esper.client.EPStatement;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
public class CdpoFogController {

    private final CepService cepService;

    public CdpoFogController(CepService cepService) {
        this.cepService = cepService;
    }

    @PostMapping("rule")
    public Map addRule(@RequestBody Map rule) {
        EPStatement stm = cepService.addRule((String) rule.get("definition"));
        stm.addListener((eventBeans, eventBeans1) -> {
            new Thread(() -> {
                Object event = eventBeans[0].getUnderlying();
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.postForObject((String) rule.get("forwardUrl"), event, Object.class);
            }).start();
        });
        return rule;
    }

}
