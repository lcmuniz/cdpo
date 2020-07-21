package br.ufma.lsdi.cdpo.models;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class DeployedRule {
    @Id
    private String uuid;
    private Rule rule;
    private String hostUuid;
    private DeployStatus status;
    private boolean enable;
}