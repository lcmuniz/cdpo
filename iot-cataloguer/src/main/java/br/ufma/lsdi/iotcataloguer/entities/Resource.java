package br.ufma.lsdi.iotcataloguer.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

@Data
@Entity
public class Resource {
    @Id
    private String uuid;
    private String name;
    private Double lat;
    private Double lon;
    @Transient
    private Gateway lastGateway;

}
