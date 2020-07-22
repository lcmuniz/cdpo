package br.ufma.lsdi.iotcataloguer.repos;

import br.ufma.lsdi.cdpo.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ResourceRepository extends MongoRepository<Resource, String> {
}
