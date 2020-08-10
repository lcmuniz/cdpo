package br.ufma.lsdi.combededgenode.models;

import lombok.Data;

import java.util.List;

@Data
public class Rule {
    private String uuid;
    private String name;
    private String description;
    private QoS qos;
    private Level level;
    private Level target;
    private String tagFilter;
    private String definition;
    private List<EventType> eventTypes;
}
