package br.ufma.lsdi.cdpo;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Resource {
    @Id
    private String uuid;
    private String name;
    private Double lat;
    private Double lon;
}
