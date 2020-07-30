package br.ufma.lsdi.cdpo.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

@Data
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Resource {

    @Id
    private String uuid;

    private String name;
    private Double lat;
    private Double lon;

    @Transient
    private Gateway lastGateway;

}
