package br.ufma.lsdi.basicfognode.models;


import lombok.Data;

@Data
public class Gateway {
    private String uuid;
    private String dn;
    private Double lat;
    private Double lon;
    private String url;
}
