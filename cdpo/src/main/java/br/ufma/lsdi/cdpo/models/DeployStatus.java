package br.ufma.lsdi.cdpo.models;

public enum DeployStatus {
    PENDING("pending"),
    DEPLOYED("deployed"),
    FAILED("failed"),
    UNDEPLOYING("undeploying"),
    UNDEPLOYED("undeployed");

    private final String status;

    DeployStatus(String status) {
        this.status = status;
    }
}
