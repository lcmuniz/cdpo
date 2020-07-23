package br.ufma.lsdi.iotcataloguer.controls;

import br.ufma.lsdi.cdpo.*;
import br.ufma.lsdi.iotcataloguer.repos.GatewayRepository;
import br.ufma.lsdi.iotcataloguer.repos.GatewayResourceRepository;
import br.ufma.lsdi.iotcataloguer.repos.ResourceRepository;
import br.ufma.lsdi.iotcataloguer.services.ResourceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("resource")
public class ResourceController {

    @Value("${cdpo.tagger.url}")
    private String taggerUrl;

    private GatewayRepository gRepo;
    private ResourceRepository rRepo;
    private GatewayResourceRepository grRepo;
    private ResourceService resourceService;

    public ResourceController(GatewayRepository gRepo, ResourceRepository rRepo, GatewayResourceRepository grRepo, ResourceService resourceService) {
        this.gRepo = gRepo;
        this.rRepo = rRepo;
        this.grRepo = grRepo;
        this.resourceService = resourceService;
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

    @PostMapping("expression")
    public List<Resource> getByExpression(@RequestBody String expression) {
        ObjectType ot = new ObjectType();
        ot.setType("EdgeNode");
        TaggedObjectFilter filter = new TaggedObjectFilter();
        filter.setObjectType(ot);
        filter.setExpression(expression);

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<TaggedObjectFilter> request = new HttpEntity<>(filter);

        ResponseEntity<List<TaggedObject>> response = restTemplate.exchange(taggerUrl + "/tagger/tagged-object/tag-expression", HttpMethod.POST, request, new ParameterizedTypeReference<List<TaggedObject>>() {});
        List<String> uuids = response.getBody().stream().map(to -> to.getUuid()).collect(Collectors.toList());

        return resourceService.findAllByUuidInWithLastGateway(uuids);

    }

    private Resource find(String uuid) {
        try {
            return rRepo.findById(uuid).get();
        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource Not Found");
        }
    }


}
