package br.ufma.lsdi.basicedgenode.models;

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
