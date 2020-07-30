package br.ufma.lsdi.iotcataloguer.controls;

import lombok.val;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class IndexController {
    @GetMapping
    public Map index() {
        val map = new HashMap();
        map.put("service-name", "iot-cataloguer");
        return map;
    }
}
