package br.ufma.lsdi.cdpo.controls;

import br.ufma.lsdi.cdpo.entities.Epn;
import br.ufma.lsdi.cdpo.repos.EpnRepository;
import br.ufma.lsdi.cdpo.services.DeployService;
import br.ufma.lsdi.cdpo.services.EpnService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/*
A classe EpnController fornece os end points 'eppn/*'
 */
@RestController
@RequestMapping("epn")
public class EpnController {

    private final EpnService epnService;
    private EpnRepository repo;
    private final DeployService service;

    public EpnController(EpnService epnService, EpnRepository repo, DeployService service) {
        this.epnService = epnService;
        this.repo = repo;
        this.service = service;
    }

    /*
    Retorna todos as epns
     */
    @GetMapping
    public List<Epn> find() {
        return repo.findAll();
    }

    /*
    Retorna uma epn indentificada pelo UUID passado como parâmetro.
    Ex: /epn/90a268cf-853c-4a5b-856d-e591a4e2467b
     */
    @GetMapping("{uuid}")
    public Optional<Epn> get(@PathVariable("uuid") String uuid) {
        return repo.findById(uuid);
    }

    /*
    Insere uma epn no banco de dados.
    Os dados são passados no corpo da requisição POST.
    Retorna a epn cadastrada.
     */
    @PostMapping
    public Epn save(@RequestBody Epn epn) {

        epnService.save(epn);
        deployEpn(epn);

        // remove, nas colecoes one-to-many, os uuids dos pais
        // para que a resposta json não tenha referência circular
        // isso é feito aqui e não no Entity porque em outros casos
        // pode ser que se queira essa referência.
        epn = removeUuids(epn);

        return epn;
    }


    /*
    Este método remove nos filhos os uuids referentes aos pais em um relacionamento
    one-to-many. Ex: Epn tem várias Rules. Rule tem o atributo epn. Esse método seta
    nulo epn. Isso é feito para casos de uso que precisem retornar as coleções de modo
    que não aja uma referẽncia circular.
     */
    private Epn removeUuids(Epn epn) {
        epn.getRules().forEach(r -> {
            r.setEpn(null);
            r.getEventTypes().forEach(et -> {
                et.setRule(null);
                et.getAttributes().forEach(att -> {
                    att.setEventType(null);
                });
            });
        });
        return epn;
    }

    /*
    Faz o deploy da epn nos hosts da rede (em uma thread separada).
     */
    private void deployEpn(Epn epn) {
        new Thread(() -> service.deploy(epn)).start();
    }

}
