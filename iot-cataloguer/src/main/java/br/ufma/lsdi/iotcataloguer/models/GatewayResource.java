package br.ufma.lsdi.iotcataloguer.models;

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
