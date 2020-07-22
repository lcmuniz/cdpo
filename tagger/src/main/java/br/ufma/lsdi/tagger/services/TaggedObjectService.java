package br.ufma.lsdi.tagger.services;

import br.ufma.lsdi.cdpo.TaggedObject;
import br.ufma.lsdi.cdpo.TaggedObjectFilter;
import br.ufma.lsdi.tagger.parsers.ParserExpression;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/*
Classe de serviço com métodos para buscas
personalizadas dos tagged objects.
 */
@Service
public class TaggedObjectService {

    private final MongoTemplate template;

    public TaggedObjectService(MongoTemplate template) {
        this.template = template;
    }

    /*
    Retorna tagged objects baseados na consulta passada como parâmetro.
    A requisição pode fornecer um type e uma expression para serem
    utilizadas como parâmetros de busca.
     */
    public List<TaggedObject> find(TaggedObjectFilter filter) {
        List exps = new ArrayList();
        if (filter.getObjectType() != null) {
            String type = "'objectType.type': '" + filter.getObjectType().getType() + "'";
            exps.add(type);
        }
        if (filter.getExpression() != null) {
            // faz o parse da expressão para o formato do MongoDB.
            String expression = ParserExpression.parse(filter.getExpression());
            exps.add(expression);
        }

        String exp = "{"+String.join(",", exps)+"}";

        BasicQuery _query = new BasicQuery(exp);
        List<TaggedObject> obs = template.find(_query, TaggedObject.class);
        return obs;
    }
}
