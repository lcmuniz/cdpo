package br.ufma.lsdi.iotcataloguer.models;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Gateway {
    @Id
    private String dn;
    private Double lat;
    private Double lon;
    private String url;
}
