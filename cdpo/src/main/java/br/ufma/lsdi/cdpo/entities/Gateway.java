package br.ufma.lsdi.cdpo.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gateway {

    @Id
    private String uuid;

    private String dn;
    private Double lat;
    private Double lon;
    private String url;

}
