package br.ufma.lsdi.cdpo.models;

public enum Level {
    CLOUD("cloud"),
    FOG("fog"),
    EDGE("edge");

    private final String level;

    Level(String level) {
        this.level = level;
    }
}
