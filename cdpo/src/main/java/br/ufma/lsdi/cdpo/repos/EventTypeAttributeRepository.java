package br.ufma.lsdi.cdpo.repos;


import br.ufma.lsdi.cdpo.entities.EventTypeAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventTypeAttributeRepository extends JpaRepository<EventTypeAttribute, String> {
}
