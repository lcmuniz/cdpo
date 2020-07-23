package br.ufma.lsdi.iotcataloguer.controls;

import br.ufma.lsdi.cdpo.*;
import br.ufma.lsdi.iotcataloguer.repos.GatewayRepository;
import br.ufma.lsdi.iotcataloguer.repos.GatewayResourceRepository;
import br.ufma.lsdi.iotcataloguer.repos.ResourceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("gateway")
public class GatewayController {

    @Value("${cdpo.tagger.url}")
    private String taggerUrl;

    private final GatewayRepository gRepo;
    private final ResourceRepository rRepo;
    private final GatewayResourceRepository grRepo;

    public GatewayController(GatewayRepository gRepo, ResourceRepository rRepo, GatewayResourceRepository grRepo) {
        this.gRepo = gRepo;
        this.rRepo = rRepo;
        this.grRepo = grRepo;
    }

    @GetMapping
    public List<Gateway> find() {
        return gRepo.findAll();
    }

    @GetMapping("dn")
    public Gateway getByDn(@RequestHeader("${iotcataloguer.dnattribute}") String dn) {
        Optional<Gateway> opt = gRepo.findByDn(dn);
        if (!opt.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gateway Not Found");
        }
        return opt.get();
    }

    @PostMapping
    public Gateway save(@RequestBody Gateway gateway, @RequestHeader("${iotcataloguer.dnattribute}") String dn) {

        Optional<Gateway> opt = gRepo.findByDn(dn);
        Gateway g;
        if (opt.isPresent()) {
            g = opt.get();
        }
        else {
            g = new Gateway();
            g.setUuid(UUID.randomUUID().toString());
            g.setDn(dn);
        }
        if (gateway.getLat() != null) g.setLat(gateway.getLat());
        if (gateway.getLon() != null) g.setLon(gateway.getLon());
        if (gateway.getUrl() != null) g.setUrl(gateway.getUrl());
        gRepo.save(g);
        return g;
    }

    @GetMapping("resources")
    public List<GatewayResource> getResources(@RequestHeader("${iotcataloguer.dnattribute}") String dn) {
        return grRepo.findAllByGateway_Dn(dn);
    }

    @PostMapping("relate")
    public GatewayResource relate(@RequestBody Resource resource, @RequestHeader("${iotcataloguer.dnattribute}") String dn) {

        Optional<Gateway> optg = gRepo.findByDn(dn);
        if (!optg.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gateway Not Found");
        }
        Gateway gateway = optg.get();

        rRepo.save(resource);

        GatewayResource gatewayResource = new GatewayResource();
        gatewayResource.setUuid(UUID.randomUUID().toString());
        gatewayResource.setTimestamp(LocalDateTime.now());
        gatewayResource.setGateway(gateway);
        gatewayResource.setResource(resource);

        grRepo.save(gatewayResource);
        return gatewayResource;
    }

    @PostMapping("expression")
    public List<Gateway> getByExpression(@RequestBody String expression) {
        ObjectType ot = new ObjectType();
        ot.setType("FogNode");
        TaggedObjectFilter filter = new TaggedObjectFilter();
        filter.setObjectType(ot);
        filter.setExpression(expression);

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<TaggedObjectFilter> request = new HttpEntity<>(filter);

        ResponseEntity<List<TaggedObject>> response = restTemplate.exchange(taggerUrl + "/tagger/tagged-object/tag-expression", HttpMethod.POST, request, new ParameterizedTypeReference<List<TaggedObject>>() {});
        List<String> uuids = response.getBody().stream().map(to -> to.getUuid()).collect(Collectors.toList());

        return gRepo.findAllByUuidIn(uuids);

    }

}
