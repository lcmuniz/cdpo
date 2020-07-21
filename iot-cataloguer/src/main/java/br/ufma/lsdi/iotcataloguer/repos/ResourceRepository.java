package br.ufma.lsdi.iotcataloguer.repos;

import br.ufma.lsdi.iotcataloguer.models.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ResourceRepository extends MongoRepository<Resource, String> {
}
