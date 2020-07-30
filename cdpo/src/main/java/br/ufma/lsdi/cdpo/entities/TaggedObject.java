package br.ufma.lsdi.cdpo.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Data
@Entity
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaggedObject {

    @Id
    private String uuid;

    private String tags;

    @ManyToOne(fetch = FetchType.LAZY)
    private ObjectType objectType;

}
