package br.ufma.lsdi.cdpo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

@Data
public class Resource {
    @Id
    private String uuid;
    private String name;
    private Double lat;
    private Double lon;
    private Gateway lastGateway;

}
