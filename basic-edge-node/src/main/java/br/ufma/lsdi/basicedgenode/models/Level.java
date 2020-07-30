package br.ufma.lsdi.basicfognode.models;

public enum Level {
    CLOUD("CLOUD"),
    FOG("FOG"),
    EDGE("EDGE");

    private final String level;

    Level(String level) {
        this.level = level;
    }
}
