package br.ufma.lsdi.iotcataloguer.repos;

import br.ufma.lsdi.iotcataloguer.entities.GatewayResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GatewayResourceRepository extends JpaRepository<GatewayResource, String> {
    List<GatewayResource> findAllByGateway_Dn(String dn);
    List<GatewayResource> findAllByResource_UuidOrderByTimestampDesc(String uuid);
}
