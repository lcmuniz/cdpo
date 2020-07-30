package br.ufma.lsdi.cdpo.repos;


import br.ufma.lsdi.cdpo.entities.Epn;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpnRepository extends JpaRepository<Epn, String> {
}
