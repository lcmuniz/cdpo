package br.ufma.lsdi.tagger.controls;

import br.ufma.lsdi.tagger.entities.ObjectType;
import br.ufma.lsdi.tagger.repos.ObjectTypeRepository;
import lombok.val;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
A classe ObjectTypeController fornece os end points 'object-type/*'
 */
@RestController
@RequestMapping("object-type")
public class ObjectTypeController {

    private ObjectTypeRepository repo;

    public ObjectTypeController(ObjectTypeRepository repo) {
        this.repo = repo;
    }

    /*
    Retorna todos os object types
     */
    @GetMapping
    public List<ObjectType> find() {
        return repo.findAll();
    }

    /*
    Retorna um object type indentificado pelo UUID passado como parâmetro.
    Ex: /object-type/90a268cf-853c-4a5b-856d-e591a4e2467b
     */
    @GetMapping("{uuid}")
    public Optional<ObjectType> get(@PathVariable("uuid") String uuid) {
        return repo.findById(uuid);
    }

    /*
    Retorna um object type identificado pelo tipo passado como parâmentro.
    O tipo do object type é único.
    Ex: /object-type/Resource
     */
    @GetMapping("type/{type}")
    public Optional<ObjectType> findbyType(@PathVariable("type") String type) {
        return repo.findByType(type);
    }

    /*
    Insere ou atualiza um object type no banco de dados.
    Os dados são passados no corpo da requisição POST.
    Retorna o object type.
     */
    @PostMapping
    public ObjectType save(@RequestBody ObjectType objectType) {

        val aSalvar = new ObjectType();
        if (objectType.getUuid() == null) {
            aSalvar.setUuid(UUID.randomUUID().toString());
        }
        else {
            aSalvar.setUuid(objectType.getUuid());
        }

        if (objectType.getType() != null) aSalvar.setType(objectType.getType());
        if (objectType.getProviderUrl() != null) aSalvar.setProviderUrl(objectType.getProviderUrl());

        return repo.save(aSalvar);
    }

}
