package br.ufma.lsdi.cdpo.models;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EventAtrributeType {

    BOOLEAN("boolean"),
    INT("int"),
    LONG("long"),
    FLOAT("float"),
    DOUBLE("double"),
    BYTES("bytes"),
    STRING("string");

    private final String type;

    EventAtrributeType(String type) {
        this.type = type;
    }
}
