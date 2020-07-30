package br.ufma.lsdi.basicedgenode.models;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Epn {
    private String uuid;
    private String commitId;
    private String version;
    private List<Rule> rules;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean enabled;
    private QoS qos;
}
