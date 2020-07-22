package br.ufma.lsdi.cdpo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.util.List;

@Data
public class TaggedObject {
    @Id
    private String uuid;
    private ObjectType objectType;
    private List<String> tags;
    @Transient
    private Gateway lastGateway;
}
