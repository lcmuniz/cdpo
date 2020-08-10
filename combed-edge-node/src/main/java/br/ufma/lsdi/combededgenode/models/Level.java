package br.ufma.lsdi.combededgenode.models;

public enum Level {
    CLOUD("CLOUD"),
    FOG("FOG"),
    EDGE("EDGE");

    private final String level;

    Level(String level) {
        this.level = level;
    }
}
