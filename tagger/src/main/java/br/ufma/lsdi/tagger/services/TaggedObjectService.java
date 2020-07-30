package br.ufma.lsdi.tagger.services;

import br.ufma.lsdi.tagger.entities.TaggedObject;
import br.ufma.lsdi.tagger.entities.TaggedObjectFilter;
import br.ufma.lsdi.tagger.repos.TaggedObjectRepository;
import lombok.val;
import lombok.var;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;

/*
Classe de serviço com métodos para buscas
personalizadas dos tagged objects.
 */
@Service
public class TaggedObjectService {

    @PersistenceContext
    private EntityManager entityManager;

    private final TaggedObjectRepository repo;

    public TaggedObjectService(TaggedObjectRepository repo) {
        this.repo = repo;
    }

    /*
    Retorna tagged objects baseados na consulta passada como parâmetro.
    A requisição pode fornecer um type e uma expression para serem
    utilizadas como parâmetros de busca.
     */
    public List<TaggedObject> find(TaggedObjectFilter filter) {
        String type = null;
        String expression = null;

        if (filter.getObjectType() != null) {
            type = "type = '" + filter.getObjectType().getType() + "'";
        }
        if (filter.getExpression() !=null) {
            expression = filter.getExpression();
            val array = expression.split(" ");

            Map<String, String> m = new HashMap<>();
            Arrays.stream(array).forEach(s -> {
                if (!s.equals("and") && !s.equals("or") && !s.equals("not")) {
                    m.put(s, "(tags like '%" + s + "%')");
                }
            });
            for (val s : m.keySet()) {
                expression = expression.replaceAll(s, m.get(s));
            }
        }

        String sql = null;
        if (type == null && expression == null) sql = "select * from tagged_object";
        if (type == null && expression != null) sql = "select * from tagged_object where " + expression;
        if (type != null && expression == null) sql = "select * from tagged_object tob join object_type ot on tob.object_type_uuid = ot.uuid where " + type;
        if (type != null && expression != null) sql = "select * from tagged_object tob join object_type ot on tob.object_type_uuid = ot.uuid where " + type + " and " + expression;

        return entityManager.createNativeQuery(sql, TaggedObject.class).getResultList();
    }
}
