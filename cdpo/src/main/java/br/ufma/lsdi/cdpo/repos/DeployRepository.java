package br.ufma.lsdi.cdpo.repos;


import br.ufma.lsdi.cdpo.entities.Deploy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeployRepository extends JpaRepository<Deploy, String> {
}
