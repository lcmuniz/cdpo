package br.ufma.lsdi.tagger.entities;

import lombok.Data;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Data
@Entity
public class TaggedObject {
    @Id
    private String uuid;
    @ManyToOne
    private ObjectType objectType;
    private String tags;
}
