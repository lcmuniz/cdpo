package br.ufma.lsdi.basicfognode.models;

import lombok.Data;

@Data
public class ObjectType {
    private String uuid;
    private String type;
    private String providerUrl;
}
