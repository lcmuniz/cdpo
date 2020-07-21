package br.ufma.lsdi.cdpo.models;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.List;

@Data
public class Rule {
    @Id
    private String uuid;
    private String name;
    private String description;
    private String tagFilter;
    private Level level;
    private Level target;
    private String definition;
    private QoS qos;
    private Epn epn;
    private List<EventAttribute> eventAttributes;
    private List<DeployedRule> deployedRules;
}
