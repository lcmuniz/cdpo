package br.ufma.lsdi.cdpo.repos;


import br.ufma.lsdi.cdpo.entities.Rule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository extends JpaRepository<Rule, String> {
}
