package br.ufma.lsdi.iotcataloguer.controls;

import br.ufma.lsdi.cdpo.GatewayResource;
import br.ufma.lsdi.cdpo.Resource;
import br.ufma.lsdi.iotcataloguer.repos.GatewayResourceRepository;
import br.ufma.lsdi.iotcataloguer.repos.ResourceRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("resource")
public class ResourceController {

    private ResourceRepository repo;
    private GatewayResourceRepository grRepo;

    public ResourceController(ResourceRepository repo, GatewayResourceRepository grRepo) {
        this.repo = repo;
        this.grRepo = grRepo;
    }

    @GetMapping
    public List<Resource> find() {
        return repo.findAll();
    }

    @GetMapping("{uuid}")
    public Optional<Resource> get(@PathVariable("uuid") String uuid) {
        return repo.findById(uuid);
    }

    @PostMapping
    public Resource insert(@RequestBody Resource resource) {
        resource.setUuid(UUID.randomUUID().toString());
        repo.save(resource);
        return resource;
    }

    @PutMapping("{uuid}")
    public Resource update(@PathVariable("uuid") String uuid, @RequestBody Resource resource) {
        Resource r = find(uuid);
        if (resource.getName() != null) r.setName(resource.getName());
        if (resource.getLat() != null) r.setLat(resource.getLat());
        if (resource.getLon() != null) r.setLon(resource.getLon());
        repo.save(r);
        return r;
    }

    private Resource find(String uuid) {
        try {
            return repo.findById(uuid).get();
        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource Not Found");
        }
    }

    @GetMapping("/{uuid}/gateways")
    public List<GatewayResource> getGateways(@PathVariable("uuid") String uuid) {
        return getGatewaysByResource(uuid);
    }

    private List<GatewayResource> getGatewaysByResource(String uuid) {
        return grRepo.findAllByResource_Uuid(uuid);
    }

}
