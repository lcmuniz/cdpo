package br.ufma.lsdi.basicedgenode.models;

import lombok.Data;

@Data
public class TaggedObjectFilter {
    private ObjectType objectType;
    private String expression;
}
