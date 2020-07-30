package br.ufma.lsdi.iotcataloguer.entities;


import lombok.Data;

import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.time.LocalDateTime;

@Data
@Entity
public class GatewayResource {
    @Id
    private String uuid;
    @ManyToOne
    private Gateway gateway;
    @ManyToOne
    private Resource resource;
    private LocalDateTime timestamp;
}

