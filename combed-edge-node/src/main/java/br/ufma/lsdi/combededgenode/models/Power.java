package br.ufma.lsdi.combededgenode.models;

import lombok.Data;
import lombok.val;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
public class Power {
    Long timestamp;
    Double power;

    public Map<String, Object> toMap(){
        val res = new HashMap<String, Object>();
        res.put("timestamp", timestamp);
        res.put("power", power);
        return res;
    }
}
