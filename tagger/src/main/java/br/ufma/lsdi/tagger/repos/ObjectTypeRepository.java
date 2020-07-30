package br.ufma.lsdi.tagger.repos;

import br.ufma.lsdi.tagger.entities.ObjectType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ObjectTypeRepository extends JpaRepository<ObjectType, String> {
    Optional<ObjectType> findByType(String type);
}
