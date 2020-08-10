package worker.LoadBalancers;


import worker.Connections.Receiver;
import worker.DatabaseAccess.Lettuce;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

public class StateUsage implements LoadBalancer {

    ConcurrentSkipListMap<String,Integer> StateRank;
    private String host;

    public StateUsage(String hostname){
        StateRank = new ConcurrentSkipListMap<>();
        host = hostname;
    }



    private void updateRank(Map<String, Receiver> receiverMap){

        Map<String,Integer> inputeventsPassed = getNumberOfInputEventsPassed(receiverMap);

        Lettuce store = new Lettuce(host);
        OrderTypesByInputEvents(inputeventsPassed,store);
        store.Close();
    }



    static Map<String,Integer> getNumberOfInputEventsPassed(Map<String, Receiver> receiverMap){
        Map<String,Integer> inputeventsPassed = new ConcurrentSkipListMap<>();
        for(String id : receiverMap.keySet()){
            inputeventsPassed.put(id,receiverMap.get(id).eventsPerPeriod());
        }
        return inputeventsPassed;
    }

    void OrderTypesByInputEvents(Map<String,Integer> InputEventsPassed,Lettuce store) {
        for(String id : StateRank.keySet()) {
            Set<String> InputsOfThisId = store.getEventTypeInputs(id);
            StateRank.put(id,0);
            for(String InputId : InputsOfThisId){
                StateRank.put(id, StateRank.get(id)+InputEventsPassed.get(InputId));
            }
        }
    }

    public String findEventTypetoRellocate() {
        return StateRank.firstKey();
    }

    public void addEventTypeToRank(String uuid, Map<String, Receiver> receiverMap){
        StateRank.put(uuid,0);
        updateRank(receiverMap);
    }

    public void deleteEventTypeofRank(String uuid, Map<String, Receiver> receiverMap){
        StateRank.remove(uuid);
        updateRank(receiverMap);
    }

}
