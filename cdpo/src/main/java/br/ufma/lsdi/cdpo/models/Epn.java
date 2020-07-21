package br.ufma.lsdi.cdpo.models;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Epn {
    @Id
    private String uuid;
    private String commitId;
    private String version;
    private List<Rule> rules;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean enabled;
    private QoS qos;
}
