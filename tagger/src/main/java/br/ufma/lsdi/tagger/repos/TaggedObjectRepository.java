package br.ufma.lsdi.tagger.repos;

import br.ufma.lsdi.cdpo.TaggedObject;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TaggedObjectRepository extends MongoRepository<TaggedObject, String> {



}
