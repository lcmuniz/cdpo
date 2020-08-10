package br.ufma.lsdi.tagger.entities;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
@NoArgsConstructor
public class ObjectType {
    @Id
    private String uuid;
    private String type;
    private String providerUrl;

    public ObjectType(String uuid, String type, String providerUrl){
        this.uuid = uuid;
        this.type = type;
        this.providerUrl = providerUrl;
    }
}
