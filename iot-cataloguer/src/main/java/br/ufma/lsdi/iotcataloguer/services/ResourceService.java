package br.ufma.lsdi.iotcataloguer.services;

import br.ufma.lsdi.cdpo.Gateway;
import br.ufma.lsdi.cdpo.GatewayResource;
import br.ufma.lsdi.cdpo.Resource;
import br.ufma.lsdi.iotcataloguer.repos.GatewayResourceRepository;
import br.ufma.lsdi.iotcataloguer.repos.ResourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceService {

    private final ResourceRepository repo;
    private final GatewayResourceRepository grRepo;

    public ResourceService(ResourceRepository repo, GatewayResourceRepository grRepo) {
        this.repo = repo;
        this.grRepo = grRepo;
    }

    public List<Resource> findAllByUuidInWithLastGateway(List<String> uuids) {
        List<Resource> rs = repo.findAllByUuidIn(uuids);
        rs.forEach(r -> {
            List<GatewayResource> grs = grRepo.findAllByResource_UuidOrderByTimestampDesc(r.getUuid());
            if (!grs.isEmpty()) {
                r.setLastGateway(grs.get(0).getGateway());
            }
        });
        return rs;
    }

}
