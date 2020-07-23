package br.ufma.lsdi.iotcataloguer.repos;

import br.ufma.lsdi.cdpo.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ResourceRepository extends MongoRepository<Resource, String> {
    List<Resource> findAllByUuidIn(List<String> uuids);
}
