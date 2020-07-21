package br.ufma.lsdi.tagger.repos;

import br.ufma.lsdi.tagger.models.ObjectType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ObjectTypeRepository extends MongoRepository<ObjectType, String> {
    Optional<ObjectType> findByType(String type);
}
