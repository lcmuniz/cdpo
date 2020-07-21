package br.ufma.lsdi.cdpo.controls;

import br.ufma.lsdi.cdpo.models.Epn;
import br.ufma.lsdi.cdpo.repos.EpnRepository;
import br.ufma.lsdi.cdpo.services.DeployService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
A classe EpnController fornece os end points 'eppn/*'
 */
@RestController
@RequestMapping("epn")
public class EpnController {

    private EpnRepository repo;
    private final DeployService service;

    public EpnController(EpnRepository repo, DeployService service) {
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
    public Epn get(@PathVariable("uuid") String uuid) {
        return findEpn(uuid);
    }

    /*
    Insere uma nova epn no banco de dados.
    Os dados são passados no corpo da requisição POST.
    Retorna a nova epn cadastrada.
     */
    @PostMapping
    public Epn insert(@RequestBody Epn epn) {
        // gera um UUID único para a nova epn
        epn.setUuid(UUID.randomUUID().toString());
        repo.save(epn);
        return epn;
    }

    /*
    Atualiza uma epn identificada pelo UUID passado como parâmetro.
    Os dados a serem atualizados são passados no corpo da requisição PUT.
    Ex: /epn/90a268cf-853c-4a5b-856d-e591a4e2467b
    Retorna a epn atualizada.
     */
    @PutMapping("{uuid}")
    public Epn update(@PathVariable("uuid") String uuid, @RequestBody Epn epn) {

        Epn e = findEpn(uuid);

        // testa cada campo para ver se foram passados na requisição.
        // dessa forma, só atualiza os campos que foram enviados.
        if (epn.getCommitId() != null) e.setCommitId(epn.getCommitId());
        if (epn.getVersion() != null) e.setVersion(epn.getVersion());
        if (epn.getRules() != null) e.setRules(epn.getRules());
        if (epn.getStartTime() != null) e.setStartTime(epn.getStartTime());
        if (epn.getEndTime() != null) e.setEndTime(epn.getEndTime());
        if (epn.getEnabled() != null) e.setEnabled(epn.getEnabled());
        if (epn.getQos() != null) e.setQos(epn.getQos());
        repo.save(e);

        deployEpn(epn);

        return e;
    }

    /*
    Faz o deploy da epn nos hosts da rede (em uma thread separada).
     */
    private void deployEpn(Epn epn) {
        new Thread(() -> service.deploy(epn)).start();
    }

    /*
    Retorna uma epn da base de dados identificada pelo UUID passado como parâmetro.
    Se a epn não existir, gera uma exceção HTTP 404
     */
    private Epn findEpn(String uuid) {
        Optional<Epn> opt = repo.findById(uuid);
        if (!opt.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Epn Not Found");
        }
        return opt.get();
    }

}
