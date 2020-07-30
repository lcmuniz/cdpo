package br.ufma.lsdi.cdpo.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.id.UUIDGenerator;

import javax.persistence.*;

@Data
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventTypeAttribute {

    @Id
    private String uuid;

    private String name;
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    private EventType eventType;

}
