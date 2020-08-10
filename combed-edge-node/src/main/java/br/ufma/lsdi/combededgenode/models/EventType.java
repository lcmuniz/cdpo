package br.ufma.lsdi.combededgenode.models;

import lombok.Data;

import java.util.List;

@Data
public class EventType {

    private String name;
    private List<EventTypeAttribute> attributes;


}
