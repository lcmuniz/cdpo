package br.ufma.lsdi.basicedgenode.models;

import lombok.Data;

@Data
public class Resource {
    private String uuid;
    private String name;
    private Double lat;
    private Double lon;
    private Gateway lastGateway;

}
