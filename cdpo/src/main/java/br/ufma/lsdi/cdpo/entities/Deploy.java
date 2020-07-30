package br.ufma.lsdi.cdpo.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Deploy {

    @Id
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    private Gateway gateway;

    @ManyToOne(fetch = FetchType.LAZY)
    private Epn epn;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Resource> resources;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Rule> rules;

}
