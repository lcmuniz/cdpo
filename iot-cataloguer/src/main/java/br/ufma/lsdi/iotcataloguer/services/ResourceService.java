package br.ufma.lsdi.iotcataloguer.services;

import br.ufma.lsdi.iotcataloguer.entities.Resource;
import br.ufma.lsdi.iotcataloguer.repos.GatewayResourceRepository;
import br.ufma.lsdi.iotcataloguer.repos.ResourceRepository;
import lombok.val;
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
        val rs = repo.findAllByUuidIn(uuids);
        rs.forEach(r -> {
            val grs = grRepo.findAllByResource_UuidOrderByTimestampDesc(r.getUuid());
            if (!grs.isEmpty()) {
                r.setLastGateway(grs.get(0).getGateway());
            }
        });
        return rs;
    }

}
