package br.ufma.lsdi.iotcataloguer.repos;

import br.ufma.lsdi.iotcataloguer.entities.Gateway;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GatewayRepository extends JpaRepository<Gateway, String> {
    Optional<Gateway> findByDn(String dn);
    List<Gateway> findAllByUuidIn(List<String> uuids);
}
