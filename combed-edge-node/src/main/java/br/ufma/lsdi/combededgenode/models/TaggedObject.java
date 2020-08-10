package br.ufma.lsdi.combededgenode.models;

import lombok.Data;

import java.util.List;

@Data
public class TaggedObject {
    private String uuid;
    private ObjectType objectType;
    private List<String> tags;
}
