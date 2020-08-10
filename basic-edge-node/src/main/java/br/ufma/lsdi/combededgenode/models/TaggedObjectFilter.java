package br.ufma.lsdi.combededgenode.models;

import lombok.Data;

@Data
public class TaggedObjectFilter {
    private ObjectType objectType;
    private String expression;
}
