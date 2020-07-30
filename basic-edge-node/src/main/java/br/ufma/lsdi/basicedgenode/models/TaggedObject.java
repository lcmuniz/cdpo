package br.ufma.lsdi.basicedgenode.models;

import lombok.Data;

import java.util.List;

@Data
public class TaggedObject {
    private String uuid;
    private ObjectType objectType;
    private List<String> tags;
}
