package br.ufma.lsdi.cdpo.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.id.UUIDGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Epn {

    @Id
    private String uuid;

    private String commitId;
    private String version;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean enabled;

    @Enumerated(EnumType.STRING)
    private QoS qos;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "epn")
    @ToString.Exclude
    private List<Rule> rules;

}
