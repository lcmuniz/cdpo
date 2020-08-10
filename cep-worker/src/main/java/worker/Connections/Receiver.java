package worker.Connections;


public interface Receiver {

    void CloseConnection();

    int eventsPerPeriod();

}
