package br.ufma.lsdi.basicfognode.models;

import lombok.Data;

@Data
public class TaggedObjectFilter {
    private ObjectType objectType;
    private String expression;
}
