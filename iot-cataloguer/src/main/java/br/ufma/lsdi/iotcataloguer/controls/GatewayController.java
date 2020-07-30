package br.ufma.lsdi.iotcataloguer.controls;

import br.ufma.lsdi.iotcataloguer.entities.*;
import br.ufma.lsdi.iotcataloguer.repos.GatewayRepository;
import br.ufma.lsdi.iotcataloguer.repos.GatewayResourceRepository;
import br.ufma.lsdi.iotcataloguer.repos.ResourceRepository;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
    public Optional<Gateway> getByDn(@RequestHeader("${iotcataloguer.dnattribute}") String dn) {
        return gRepo.findByDn(dn);
    }

    @PostMapping
    public Gateway save(@RequestBody Gateway gateway, @RequestHeader("${iotcataloguer.dnattribute}") String dn) {
        val opt = gRepo.findByDn(dn);
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
        return gRepo.save(g);
    }

    @GetMapping("resources")
    public List<GatewayResource> getResources(@RequestHeader("${iotcataloguer.dnattribute}") String dn) {
        return grRepo.findAllByGateway_Dn(dn);
    }

    @PostMapping("relate")
    public GatewayResource relate(@RequestBody Resource resource, @RequestHeader("${iotcataloguer.dnattribute}") String dn) {

        val optg = gRepo.findByDn(dn);
        if (!optg.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gateway Not Found");
        }
        val gateway = optg.get();

        val aSalvar = new Resource();
        if (resource.getUuid() == null) {
            // se uuid do recurso nao esta presente, cria um novo recurso
            aSalvar.setUuid(UUID.randomUUID().toString());
        }
        else {
            aSalvar.setUuid(resource.getUuid());
            // se uuid esta presente na requisicao, busca no banco
            val optr = rRepo.findById(resource.getUuid());
            if (optr.isPresent()) {
                // se encontrou no banco
                aSalvar.setName(optr.get().getName());
                aSalvar.setLat(optr.get().getLat());
                aSalvar.setLon(optr.get().getLon());
            }
        }

        // atualiza os campos do recurso a salvar
        if (resource.getName() != null) aSalvar.setName(resource.getName());
        if (resource.getLat() != null) aSalvar.setLat(resource.getLat());
        if (resource.getLon() != null) aSalvar.setLon(resource.getLon());

        resource = rRepo.save(aSalvar);

        val gatewayResource = new GatewayResource();
        gatewayResource.setUuid(UUID.randomUUID().toString());
        gatewayResource.setTimestamp(LocalDateTime.now());
        gatewayResource.setGateway(gateway);
        gatewayResource.setResource(resource);

        return grRepo.save(gatewayResource);
    }

    @PostMapping("expression")
    public List<Gateway> getByExpression(@RequestBody String expression) {
        val ot = new ObjectType();
        ot.setType("FogNode");
        val filter = new TaggedObjectFilter();
        filter.setObjectType(ot);
        filter.setExpression(expression);

        val restTemplate = new RestTemplate();
        val request = new HttpEntity<>(filter);

        val response = restTemplate.exchange(taggerUrl + "/tagger/tagged-object/tag-expression", HttpMethod.POST, request, new ParameterizedTypeReference<List<TaggedObject>>() {});
        val uuids = response.getBody().stream().map(to -> to.getUuid()).collect(Collectors.toList());

        return gRepo.findAllByUuidIn(uuids);

    }

}
