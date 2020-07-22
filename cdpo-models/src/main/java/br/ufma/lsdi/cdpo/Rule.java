package br.ufma.lsdi.cdpo;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.List;

@Data
public class Rule {
    @Id
    private String uuid;
    private String name;
    private Double description;
    private QoS qos;
    private Level level;
    private Level target;
    private String tagFilter;
    private String definition;
    private List<EventType> eventTypes;

    private List<Deploy> deploys;
}
