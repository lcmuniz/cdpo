package br.ufma.lsdi.cdpo.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Rule {

    @Id
    private String uuid;

    private String name;
    private String description;
    private String tagFilter;
    private String definition;

    @Enumerated(EnumType.STRING)
    private QoS qos;

    @Enumerated(EnumType.STRING)
    private Level level;

    @Enumerated(EnumType.STRING)
    private Level target;

    @ManyToOne(fetch = FetchType.LAZY)
    private Epn epn;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "rule")
    @ToString.Exclude
    private List<EventType> eventTypes;

}
