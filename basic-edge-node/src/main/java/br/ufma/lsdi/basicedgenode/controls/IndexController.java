package br.ufma.lsdi.basicedgenode.controls;

import lombok.val;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * A classe IndexController fornece apenas um end point que anuncia o nome da applicação.
 */
@RestController
public class IndexController {

    /*
    End point que retorna um objeto JSON com o nome da aplicação.
     */
    @GetMapping
    public Map index() {
        val map = new HashMap();
        map.put("app-name", "basic-edge-node");
        return map;
    }
}
