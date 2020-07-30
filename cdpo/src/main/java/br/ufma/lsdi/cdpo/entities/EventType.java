package br.ufma.lsdi.cdpo.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;
import org.hibernate.id.UUIDGenerator;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventType {

    @Id
    private String uuid;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private Rule rule;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "eventType")
    @ToString.Exclude
    private List<EventTypeAttribute> attributes;

}
