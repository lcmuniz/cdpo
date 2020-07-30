package br.ufma.lsdi.iotcataloguer.repos;


import br.ufma.lsdi.iotcataloguer.entities.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, String> {
    List<Resource> findAllByUuidIn(List<String> uuids);
}
