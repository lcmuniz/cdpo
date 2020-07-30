package br.ufma.lsdi.iotcataloguer.entities;

import lombok.Data;

@Data
public class TaggedObjectFilter {
    private ObjectType objectType;
    private String expression;
}
