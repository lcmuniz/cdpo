package br.ufma.lsdi.tagger.services;

import br.ufma.lsdi.tagger.models.TaggedObject;
import br.ufma.lsdi.tagger.parsers.ParserExpression;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public List<TaggedObject> find(Map query) {
        List exps = new ArrayList();
        if (query.get("type") != null) {
            String type = "'objectType.type': '" + query.get("type").toString() + "'";
            exps.add(type);
        }
        if (query.get("expression") != null) {
            // faz o parse da expressão para o formato do MongoDB.
            String expression = ParserExpression.parse(query.get("expression").toString());
            exps.add(expression);
        }

        String exp = "{"+String.join(",", exps)+"}";

        BasicQuery _query = new BasicQuery(exp);
        return template.find(_query, TaggedObject.class);
    }
}
