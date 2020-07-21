package br.ufma.lsdi.iotcataloguer.repos;

import br.ufma.lsdi.iotcataloguer.models.Gateway;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface GatewayRepository extends MongoRepository<Gateway, String> {
    Optional<Gateway> findByDn(String dn);
}
