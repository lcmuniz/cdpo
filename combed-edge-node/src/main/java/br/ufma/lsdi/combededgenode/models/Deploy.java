package br.ufma.lsdi.combededgenode.models;

import lombok.Data;

import java.util.List;

@Data
public class Deploy {
    private String uuid;
    private Gateway gateway;
    private List<Resource> resources;
    private Epn epn;
    private List<Rule> rules;
}
