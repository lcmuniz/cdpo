package br.ufma.lsdi.cdpo;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.List;

@Data
public class Deploy {
    @Id
    private String uuid;
    private Gateway gateway;
    private List<Resource> resources;
    private Epn epn;
    private List<Rule> rules;
}
