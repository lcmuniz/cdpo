package br.ufma.lsdi.iotcataloguer.controls;

import br.ufma.lsdi.cdpo.Gateway;
import br.ufma.lsdi.cdpo.GatewayResource;
import br.ufma.lsdi.cdpo.Resource;
import br.ufma.lsdi.iotcataloguer.repos.GatewayRepository;
import br.ufma.lsdi.iotcataloguer.repos.GatewayResourceRepository;
import br.ufma.lsdi.iotcataloguer.repos.ResourceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("resource")
public class ResourceController {

    private GatewayRepository gRepo;
    private ResourceRepository rRepo;
    private GatewayResourceRepository grRepo;

    public ResourceController(GatewayRepository gRepo, ResourceRepository rRepo, GatewayResourceRepository grRepo) {
        this.gRepo = gRepo;
        this.rRepo = rRepo;
        this.grRepo = grRepo;
    }

    @GetMapping
    public List<Resource> find() {
        return rRepo.findAll();
    }

    @GetMapping("{uuid}")
    public Optional<Resource> get(@PathVariable("uuid") String uuid) {
        return rRepo.findById(uuid);
    }

    @PostMapping
    public Resource insert(@RequestBody Resource resource) {
        resource.setUuid(UUID.randomUUID().toString());
        rRepo.save(resource);
        return resource;
    }

    @PutMapping("{uuid}")
    public Resource update(@PathVariable("uuid") String uuid, @RequestBody Resource resource) {
        Resource r = find(uuid);
        if (resource.getName() != null) r.setName(resource.getName());
        if (resource.getLat() != null) r.setLat(resource.getLat());
        if (resource.getLon() != null) r.setLon(resource.getLon());
        rRepo.save(r);
        return r;
    }

    private Resource find(String uuid) {
        try {
            return rRepo.findById(uuid).get();
        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource Not Found");
        }
    }

    @GetMapping("/{uuid}/last-gateway")
    public Gateway getGateways(@PathVariable("uuid") String uuid) {
        List<GatewayResource> list = grRepo.findAllByResource_UuidOrderByTimestampDesc(uuid);
        if (list.size() > 0) {
            GatewayResource gr = list.get(0);
            Optional<Gateway> opt = gRepo.findById(gr.getGateway().getUuid());
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        return null;
    }

}
