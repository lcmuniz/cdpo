package br.ufma.lsdi.cdpo.controls;

import lombok.val;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * A classe IndexController fornece apenas um end point que anuncia o nome do serviço.
 * (Para que se possa verificar se o serviço está ativo).
 */
@RestController
public class IndexController {

    /*
    End point que retorna um objeto JSON com o nome do serviço.
     */
    @GetMapping
    public Map index() {
        val map = new HashMap();
        map.put("service-name", "cdpo");
        return map;
    }
}
