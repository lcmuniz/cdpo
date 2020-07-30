package br.ufma.lsdi.cdpo.repos;


import br.ufma.lsdi.cdpo.entities.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventTypeRepository extends JpaRepository<EventType, String> {
}
