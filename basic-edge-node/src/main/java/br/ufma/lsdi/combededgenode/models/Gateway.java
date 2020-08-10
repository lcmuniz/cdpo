package br.ufma.lsdi.combededgenode.models;


import lombok.Data;

@Data
public class Gateway {
    private String uuid;
    private String dn;
    private Double lat;
    private Double lon;
    private String url;
}
