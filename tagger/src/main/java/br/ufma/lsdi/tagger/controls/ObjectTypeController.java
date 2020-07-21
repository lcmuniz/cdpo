package br.ufma.lsdi.tagger.controls;

import br.ufma.lsdi.tagger.models.ObjectType;
import br.ufma.lsdi.tagger.repos.ObjectTypeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public ObjectType get(@PathVariable("uuid") String uuid) {
        return findObjectType(uuid);
    }

    /*
    Retorna um object type identificado pelo tipo passado como parâmentro.
    O tipo do object type é único.
    Ex: /object-type/Resource
     */
    @GetMapping("type/{type}")
    public ObjectType findbyType(@PathVariable("type") String type) {
        Optional<ObjectType> opt = repo.findByType(type);
        if (!opt.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Object Type Not Found");
        }
        return opt.get();
    }

    /*
    Insere um novo object type no banco de dados.
    Os dados são passados no corpo da requisição POST.
    Retorna o novo object type cadastrado.
     */
    @PostMapping
    public ObjectType insert(@RequestBody ObjectType objectType) {
        // gera um UUID único para o novo object type
        objectType.setUuid(UUID.randomUUID().toString());
        repo.save(objectType);
        return objectType;
    }

    /*
    Atualiza um object type identificado pelo UUID passado como parâmetro.
    Os dados a serem atualizados são passados no corpo da requisição PUT.
    Ex: /object-type/90a268cf-853c-4a5b-856d-e591a4e2467b
    Retorna o object type atualizado.
     */
    @PutMapping("{uuid}")
    public ObjectType update(@PathVariable("uuid") String uuid, @RequestBody ObjectType objectType) {

        ObjectType ot = findObjectType(uuid);

        // testa cada campo para ver se foram passados na requisição.
        // dessa forma, só atualiza os campos que foram enviados.
        if (objectType.getType() != null) ot.setType(objectType.getType());
        if (objectType.getProviderUrl() != null) ot.setProviderUrl(objectType.getProviderUrl());
        repo.save(ot);
        return ot;
    }

    /*
    Retorna um object type da base de dados identificado  pelo UUID passado como parâmetro.
    Se o object type não existir, gera uma exceção HTTP 404
     */
    private ObjectType findObjectType(String uuid) {
        Optional<ObjectType> opt = repo.findById(uuid);
        if (!opt.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Object Type Not Found");
        }
        return opt.get();
    }

}
