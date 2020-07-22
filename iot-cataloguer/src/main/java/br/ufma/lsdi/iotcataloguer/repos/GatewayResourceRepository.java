package br.ufma.lsdi.iotcataloguer.repos;

import br.ufma.lsdi.cdpo.GatewayResource;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface GatewayResourceRepository extends MongoRepository<GatewayResource, String> {
    List<GatewayResource> findAllByGateway_Dn(String dn);
    List<GatewayResource> findAllByResource_UuidOrderByTimestampDesc(String uuid);
}
