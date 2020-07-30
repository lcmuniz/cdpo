package br.ufma.lsdi.cdpo;

import lombok.Data;

import java.util.List;

@Data
public class EventType {

    private String name;
    private List<EventTypeAttribute> attributes;


}
