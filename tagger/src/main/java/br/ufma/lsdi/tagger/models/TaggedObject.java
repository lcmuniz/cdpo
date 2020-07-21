package br.ufma.lsdi.tagger.models;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.util.List;

@Data
public class TaggedObject {

    @Id
    private String uuid;
    private ObjectType objectType;
    private List<String> tags;

}
