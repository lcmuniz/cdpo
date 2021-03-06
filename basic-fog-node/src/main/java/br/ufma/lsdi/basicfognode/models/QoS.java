package br.ufma.lsdi.basicfognode.models;

public enum QoS {
    AT_MOST_ONCE("at_most_once"),
    AT_LAST_ONCE("at_last_once"),
    EXACTLY_ONCE("exactly_once");

    private final String qos;

    QoS(String qos) {
        this.qos = qos;
    }
}
