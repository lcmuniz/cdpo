package br.ufma.lsdi.iotcataloguer.entities;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class ObjectType {
    @Id
    private String uuid;
    private String type;
    private String providerUrl;
}
