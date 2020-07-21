package br.ufma.lsdi.tagger.parsers;

import java.util.HashMap;
import java.util.Map;

/*
Esta classe é utilizada pela classe TaggedObjectController para fazer o parser
da expressão que o usuário passa na requisição /tagged-object/ta-expression.
O usuário pode passar uma expressão (por exemplo: tag1 and tag2 or tag3) e este parser
transforma essa expressão em uma query para o MongoDB
(por exemplo: {$or:[{$and:[{tags: {$in:['tag1']}},{tags: {$in:['tag2']}}]},{tags: {$in:['tag3']}}]}})
 */
public class ParserExpression {


    public static String parse(String expression) {

        // remove todos as palavras reservados e sinais de ( e ) para ficar apenas com as tags.
        String t1 = expression
                .replace("and", "")
                .replace("or","")
                .replace("(", "")
                .replace(")", "")
                .replaceAll(" +"," ")
                .replace("not ", "not#");
        String[] a1 = t1.split(" ");

        // substitui as tags com not na frente por not#tag
        String exp = expression.replace("not ", "not#");

        // guarda as tag em um map com chaves alfabéticas.
        // por exemplo: a primeira tag fica na chave A, a segunda na B, etc.
        Map<Character, String> map = new HashMap<>();
        for (int i = 0; i < a1.length; i++) {
            char c = (char) (i + 65);
            map.put(c, a1[i]);
            exp = exp.replace(a1[i], c+"");
        }

        // altera os conectores lógicos 'and', 'or' por '&' e '|' para a classe GFG
        exp = exp
                .replace("and", "&")
                .replace("or", "|")
                .replace(" ", "");

        // a classe GFG retorna a expressão na forma infixada (A+B -> +AB)
        String resp = GFG.infixToPrefix(exp);

        // substitui os conectores lógicos pela forma da query do MongoDB
        String e = resp
                .replace("&", "$and:")
                .replace("|", "$or:");

        // substitui as letras na expressão infixada pelas tags que foram quardadas
        // no map, já convertendo para o formato da query do MongoDB.
        for(Character c: map.keySet()) {
            if (map.get(c).contains("not#")) {
                e = e.replace(c + "", "{tags: {$nin:['" + map.get(c).replace("not#", "") + "']}}");
            }
            else {
                e = e.replace(c + "", "{tags: {$in:['" + map.get(c) + "']}}");
            }

        }
        // remove os { e } iniciais e finais.
        e = e.substring(1, e.length());

        // retorna a query no formato do MongoDB
        return e;
    }
}
