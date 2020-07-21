package br.ufma.lsdi.cdpo.models;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class EventAttribute {
    @Id
    private String id;
    private Rule rule;
    private String name;
    private EventAtrributeType type;
}
