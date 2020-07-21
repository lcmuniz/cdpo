package br.ufma.lsdi.iotcataloguer.controls;

import br.ufma.lsdi.iotcataloguer.models.Gateway;
import br.ufma.lsdi.iotcataloguer.models.GatewayResource;
import br.ufma.lsdi.iotcataloguer.models.Resource;
import br.ufma.lsdi.iotcataloguer.repos.GatewayRepository;
import br.ufma.lsdi.iotcataloguer.repos.GatewayResourceRepository;
import br.ufma.lsdi.iotcataloguer.repos.ResourceRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("gateway")
public class GatewayController {

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

    @GetMapping("get")
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
            g.setDn(gateway.getDn());
        }
        if (gateway.getLat() != null) g.setLat(gateway.getLat());
        if (gateway.getLon() != null) g.setLon(gateway.getLon());
        if (gateway.getUrl() != null) g.setUrl(gateway.getUrl());
        gRepo.save(g);
        return g;
    }

    @GetMapping("resources")
    public List<ResourceResponse> getResources(@RequestHeader("${iotcataloguer.dnattribute}") String dn) {
        return getResourcesByGateway(dn);
    }

    @PostMapping("relate")
    public GatewayResource relate(@RequestBody Map<String, Object> request, @RequestHeader("${iotcataloguer.dnattribute}") String dn) {

        String resourceUuid = (String) request.get("uuid");
        String resourceName = (String) request.get("name");
        Double resoureLat = (Double) request.get("lat");
        Double resourceLon = (Double) request.get("lon");

        Optional<Gateway> optg = gRepo.findByDn(dn);
        if (!optg.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gateway Not Found");
        }
        Gateway gateway = optg.get();

        Optional<Resource> optr = rRepo.findById(resourceUuid);
        Resource resource;
        if (optr.isPresent()) {
            resource = optr.get();
        }
        else {
            resource = new Resource();
            resource.setUuid(resourceUuid);
        }
        resource.setName(resourceName);
        resource.setLat(resoureLat);
        resource.setLon(resourceLon);
        rRepo.save(resource);

        GatewayResource gatewayResource = new GatewayResource();
        gatewayResource.setUuid(UUID.randomUUID().toString());
        gatewayResource.setTimestamp(LocalDateTime.now());
        gatewayResource.setGateway(gateway);
        gatewayResource.setResource(resource);

        grRepo.save(gatewayResource);
        return gatewayResource;
    }

    private List<ResourceResponse> getResourcesByGateway(String dn) {
        List<GatewayResource> grs = grRepo.findAllByGateway_Dn(dn);
        return grs.stream().map(gr -> new ResourceResponse(
                gr.getResource().getUuid(),
                gr.getResource().getName(),
                gr.getResource().getLat(),
                gr.getResource().getLon(),
                gr.getTimestamp()
        )).collect(Collectors.toList());
    }

}

@Data
@AllArgsConstructor
class ResourceResponse {
    private String uuid;
    private String name;
    private Double lat;
    private Double lon;
    private LocalDateTime timestamp;
}