package br.ufma.lsdi.basicfognode.models;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GatewayResource {
    private String uuid;
    private Gateway gateway;
    private Resource resource;
    private LocalDateTime timestamp;
}

