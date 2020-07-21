package br.ufma.lsdi.cdpo.repos;

import br.ufma.lsdi.cdpo.models.Epn;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TaggedObjectRepository extends MongoRepository<Epn, String> {

}
