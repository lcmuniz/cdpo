package br.ufma.lsdi.cdpo;


import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Data
public class GatewayResource {
    @Id
    private String uuid;
    private Gateway gateway;
    private Resource resource;
    private LocalDateTime timestamp;
}

