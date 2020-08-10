package worker.LoadBalancers;


import worker.Connections.Receiver;

import java.util.Map;


public interface LoadBalancer {

    String findEventTypetoRellocate();

    void addEventTypeToRank(String uuid, Map<String, Receiver> receiverMap);

    void deleteEventTypeofRank(String uuid, Map<String, Receiver> receiverMap);

}
