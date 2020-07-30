package br.ufma.lsdi.cdpo;


import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class Gateway {
    @Id
    private String uuid;
    private String dn;
    private Double lat;
    private Double lon;
    private String url;
}
