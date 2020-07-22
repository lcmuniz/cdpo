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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
            g.setUuid(UUID.randomUUID().toString());
            g.setDn(gateway.getDn());
        }
        if (gateway.getLat() != null) g.setLat(gateway.getLat());
        if (gateway.getLon() != null) g.setLon(gateway.getLon());
        if (gateway.getUrl() != null) g.setUrl(gateway.getUrl());
        gRepo.save(g);
        return g;
    }

    @GetMapping("resources")
    public List<GatewayResource> getResources(@RequestHeader("${iotcataloguer.dnattribute}") String dn) {
        return getResourcesByGateway(dn);
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

    private List<GatewayResource> getResourcesByGateway(String dn) {
        return grRepo.findAllByGateway_Dn(dn);
    }

}
