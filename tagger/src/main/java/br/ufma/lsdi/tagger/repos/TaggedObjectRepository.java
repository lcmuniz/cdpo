package br.ufma.lsdi.tagger.repos;

import br.ufma.lsdi.tagger.entities.TaggedObject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaggedObjectRepository extends JpaRepository<TaggedObject, String> {
}
