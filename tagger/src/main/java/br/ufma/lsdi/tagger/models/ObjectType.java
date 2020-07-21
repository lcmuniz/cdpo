package br.ufma.lsdi.tagger.models;


import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class ObjectType {

    @Id
    private String uuid;
    private String type;
    private String providerUrl;

}
