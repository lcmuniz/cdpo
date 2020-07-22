package br.ufma.lsdi.tagger.controls;

import br.ufma.lsdi.cdpo.Gateway;
import br.ufma.lsdi.cdpo.ObjectType;
import br.ufma.lsdi.cdpo.TaggedObject;
import br.ufma.lsdi.cdpo.TaggedObjectFilter;
import br.ufma.lsdi.tagger.repos.ObjectTypeRepository;
import br.ufma.lsdi.tagger.repos.TaggedObjectRepository;
import br.ufma.lsdi.tagger.services.TaggedObjectService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/*
A classe TaggedObjectController fornece os end points 'tagged-object/*'
 */
@RestController
@RequestMapping("tagged-object")
public class TaggedObjectController {

    @Value("${cdpo.iotcataloguer.url}")
    private String iotCataloguerUrl;

    private final TaggedObjectService serv;
    private TaggedObjectRepository repo;
    private ObjectTypeRepository otRepo;

    public TaggedObjectController(TaggedObjectService serv, TaggedObjectRepository repo, ObjectTypeRepository otRepo) {
        this.serv = serv;
        this.repo = repo;
        this.otRepo = otRepo;
    }

    /*
    Retorna todos os tagged objects
     */
    @GetMapping
    public List<TaggedObject> find() {
        return repo.findAll();
    }

    /*
    Retorna um tagged object indentificado pelo UUID passado como parâmetro.
    Ex: /tagged-object/90a268cf-853c-4a5b-856d-e591a4e2467b
     */
    @GetMapping("{uuid}")
    public TaggedObject get(@PathVariable("uuid") String uuid) {
        TaggedObject to = findTaggedObject(uuid);
        if (to.getObjectType().getType().equals("EdgeNode")) {
            // busca o ultimo gateway associado
            Gateway lastGateway = findLastGateway(uuid);
            to.setLastGateway(lastGateway);
        }
        return to;
    }

    /*
    Insere ou atualiza um tagged object no banco de dados.
    Os dados são passados no corpo da requisição POST.
    Retorna o tagged object.
     */
    @PostMapping
    public TaggedObject save(@RequestBody TaggedObject taggedObject) {

        if (taggedObject.getUuid() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Object Type UUID is Required");
        }

        Optional<TaggedObject> opt = repo.findById(taggedObject.getUuid());

        TaggedObject to;
        if (opt.isPresent()) {
            to = opt.get();
        }
        else {
            to = new TaggedObject();
            to.setUuid(taggedObject.getUuid());
        }
        // testa se o object type foi passado na requisição
        if (taggedObject.getObjectType() != null) {
            // se sim, busca o object type no banco e preenche no tagged object
            ObjectType ot = findObjectType(taggedObject.getObjectType().getUuid());
            to.setObjectType(ot);
        }
        if (taggedObject.getTags() != null) to.setTags(taggedObject.getTags());
        repo.save(to);
        return to;
    }

    /*
    Retorna os tagged objects filtrados pela expressão passada na requisição.
     */
    @PostMapping("tag-expression")
    public List<TaggedObject> findbyExpression(@RequestBody TaggedObjectFilter expression) {
        List<TaggedObject> tos = serv.find(expression);
        for (TaggedObject to : tos) {
            to.setLastGateway(findLastGateway(to.getUuid()));
        }
        return tos;
    }

    /*
    Retorna um tagged object da base de dados identificado  pelo UUID passado como parâmetro.
    Se o tagged object não existir, gera uma exceção HTTP 404
     */
    private TaggedObject findTaggedObject(String uuid) {
        Optional<TaggedObject> opt = repo.findById(uuid);
        if (!opt.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tagged Object Not Found");
        }
        return opt.get();
    }

    /*
    Retorna um object type da base de dados identificado  pelo UUID passado como parâmetro.
    Se o object type não existir, gera uma exceção HTTP 404
     */
    private ObjectType findObjectType(String uuid) {
        try {
            return otRepo.findById(uuid).get();
        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Object Type Not Found");
        }
    }

    // busca o ultimo gateway associado
    private Gateway findLastGateway(String uuid) {
        RestTemplate restTemplate = new RestTemplate();
        Gateway lastGateway = restTemplate.getForObject(iotCataloguerUrl + "/iot-cataloguer/resource/" + uuid + "/last-gateway", Gateway.class);
        return lastGateway;
    }

}
