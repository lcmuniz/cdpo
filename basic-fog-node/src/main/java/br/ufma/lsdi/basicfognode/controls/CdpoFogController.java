package br.ufma.lsdi.basicfognode.controls;

import br.ufma.lsdi.basicfognode.models.Deploy;
import br.ufma.lsdi.basicfognode.models.Resource;
import br.ufma.lsdi.basicfognode.models.Rule;
import br.ufma.lsdi.basicfognode.services.CdpoFogService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
Esta classe representa o serviço rest do fog node.
Ela é responsável por receber as regras a serem processadas
pelo fog nodes e seus edge nodes associados.
 */
@RestController
public class CdpoFogController {

    private final CdpoFogService cdpoFogService;

    public CdpoFogController(CdpoFogService cdpoFogService) {
        this.cdpoFogService = cdpoFogService;
    }


    /*
    Recebe os deploys para regras fog
     */
    @PostMapping("deploy-fog")
    public Deploy deployFog(@RequestBody Deploy deploy) {
        deploy.getRules().forEach(rule -> cdpoFogService.processInFog(rule));
        return deploy;
    }

    /*
    Recebe os deploys para regras fog
     */
    @PostMapping("deploy-edge")
    public Deploy deployEdge(@RequestBody Deploy deploy) {
        deploy.getResources().forEach(resource -> sendRulesToEdge(resource, deploy.getRules()));
        return deploy;
    }

    /*
    Recebe os deploys para regras cloud
     */
    @PostMapping("deploy-cloud")
    public Deploy deployCloud(@RequestBody Deploy deploy) {
        //deploy.getResources().forEach(resource -> sendRulesToEdge(resource, deploy.getRules()));
        return deploy;
    }

    /*
    Envia as regras para um resource
    */
    private void sendRulesToEdge(Resource resource, List<Rule> rules) {
        cdpoFogService.sendRuleToEdge(resource, rules);
    }


}
