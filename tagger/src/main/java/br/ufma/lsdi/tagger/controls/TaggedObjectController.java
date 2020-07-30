package br.ufma.lsdi.tagger.controls;

import br.ufma.lsdi.tagger.entities.TaggedObject;
import br.ufma.lsdi.tagger.entities.TaggedObjectFilter;
import br.ufma.lsdi.tagger.repos.ObjectTypeRepository;
import br.ufma.lsdi.tagger.repos.TaggedObjectRepository;
import br.ufma.lsdi.tagger.services.TaggedObjectService;
import lombok.val;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/*
A classe TaggedObjectController fornece os end points 'tagged-object/*'
 */
@RestController
@RequestMapping("tagged-object")
public class TaggedObjectController {

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
    public Optional<TaggedObject> get(@PathVariable("uuid") String uuid) {
        return repo.findById(uuid);
    }

    /*
    Insere ou atualiza um tagged object no banco de dados.
    Os dados são passados no corpo da requisição POST.
    Retorna o tagged object.
     */
    @PostMapping
    public TaggedObject save(@RequestBody TaggedObject taggedObject) {
        val aSalvar = new TaggedObject();
        if (taggedObject.getUuid() == null) {
            aSalvar.setUuid(UUID.randomUUID().toString());
        }
        else {
            aSalvar.setUuid(taggedObject.getUuid());
        }

        if(taggedObject.getObjectType() != null) aSalvar.setObjectType(taggedObject.getObjectType());
        if(taggedObject.getTags() != null) aSalvar.setTags(taggedObject.getTags());

        return repo.save(taggedObject);
    }

    /*
    Retorna os tagged objects filtrados pela expressão passada na requisição.
     */
    @PostMapping("tag-expression")
    public List<TaggedObject> findbyExpression(@RequestBody TaggedObjectFilter expression) {
        return serv.find(expression);
    }

}
