package br.ufma.lsdi.iotcataloguer.controls;

import br.ufma.lsdi.iotcataloguer.entities.*;
import br.ufma.lsdi.iotcataloguer.repos.GatewayRepository;
import br.ufma.lsdi.iotcataloguer.repos.GatewayResourceRepository;
import br.ufma.lsdi.iotcataloguer.repos.ResourceRepository;
import br.ufma.lsdi.iotcataloguer.services.ResourceService;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

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
    public Resource save (@RequestBody Resource resource) {
        val aSalvar  = new Resource();
        if (resource.getUuid() == null) {
            aSalvar.setUuid(UUID.randomUUID().toString());
        }
        else {
            aSalvar.setUuid(resource.getUuid());
        }

        if(resource.getName() != null) aSalvar.setName(resource.getName());
        if(resource.getLat() != null) aSalvar.setLat(resource.getLat());
        if(resource.getLon() != null) aSalvar.setLon(resource.getLon());

        return rRepo.save(resource);
    }

    @GetMapping("/{uuid}/last-gateway")
    public Gateway getGateways(@PathVariable("uuid") String uuid) {
        val list = grRepo.findAllByResource_UuidOrderByTimestampDesc(uuid);
        if (list.size() > 0) {
            val gr = list.get(0);
            val opt = gRepo.findById(gr.getGateway().getUuid());
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        return null;
    }

    @PostMapping("expression")
    public List<Resource> getByExpression(@RequestBody String expression) {
        val ot = new ObjectType();
        ot.setType("EdgeNode");
        val filter = new TaggedObjectFilter();
        filter.setObjectType(ot);
        filter.setExpression(expression);

        val restTemplate = new RestTemplate();
        val request = new HttpEntity<>(filter);

        val response = restTemplate.exchange(taggerUrl + "/tagger/tagged-object/tag-expression", HttpMethod.POST, request, new ParameterizedTypeReference<List<TaggedObject>>() {});
        val uuids = response.getBody().stream().map(to -> to.getUuid()).collect(Collectors.toList());

        return resourceService.findAllByUuidInWithLastGateway(uuids);

    }

}
