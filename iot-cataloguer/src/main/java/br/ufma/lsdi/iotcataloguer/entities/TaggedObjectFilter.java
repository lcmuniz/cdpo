package br.ufma.lsdi.cdpo;

import lombok.Data;

@Data
public class TaggedObjectFilter {
    private ObjectType objectType;
    private String expression;
}
