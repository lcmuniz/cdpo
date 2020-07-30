package br.ufma.lsdi.iotcataloguer.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class Gateway {
    @Id
    private String uuid;
    private String dn;
    private Double lat;
    private Double lon;
    private String url;
}
