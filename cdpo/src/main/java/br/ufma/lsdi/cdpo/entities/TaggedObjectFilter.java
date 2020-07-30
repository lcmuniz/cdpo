package br.ufma.lsdi.cdpo.entities;

import lombok.Data;

@Data
public class TaggedObjectFilter {
    private ObjectType objectType;
    private String expression;
}
