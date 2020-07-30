package br.ufma.lsdi.cdpo.entities;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.time.LocalDateTime;

@Data
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GatewayResource {

    @Id
    private String uuid;

    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    private Gateway gateway;

    @ManyToOne(fetch = FetchType.LAZY)
    private Resource resource;

}

