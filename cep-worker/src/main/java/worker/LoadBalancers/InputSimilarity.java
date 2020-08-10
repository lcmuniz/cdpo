package worker.LoadBalancers;


import worker.Connections.Receiver;
import worker.DatabaseAccess.Lettuce;


import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class InputSimilarity implements LoadBalancer{

    private ConcurrentSkipListMap<String,Integer> inputEventRank;
    private ConcurrentSkipListSet<String> eventTypeIds;
    private String host;

    public InputSimilarity(String hostname){
        inputEventRank = new ConcurrentSkipListMap<>();
        eventTypeIds = new ConcurrentSkipListSet<>();
        host = hostname;
    }


    public String findEventTypetoRellocate() { // finds the event type that uses the least used input
        Lettuce store = new Lettuce(host);
        String leastSimilarEventTypeId = findTypetoRellocate(store);
        store.Close();
        return leastSimilarEventTypeId;
    }

    public void addEventTypeToRank(String uuid, Map<String, Receiver> receiverMap){
        Lettuce store = new Lettuce(host);
        addTypeToRank(uuid,store);
        store.Close();
    }

    public void deleteEventTypeofRank(String uuid, Map<String, Receiver> receiverMap){
        Lettuce store = new Lettuce(host);
        deleteTypeofRank(uuid,store);
        store.Close();
    }

    void addTypeToRank(String uuid, Lettuce store){


        for (String input : store.getEventTypeInputs(uuid)) {
            if (inputEventRank.containsKey(input)) {
                int i = inputEventRank.get(input);
                inputEventRank.put(input, ++i);
            } else {
                inputEventRank.put(input, 1);
            }
        }
        this.eventTypeIds.add(uuid);
    }

    void deleteTypeofRank(String uuid,Lettuce store){

        for (String input : store.getEventTypeInputs(uuid)) {
            if (inputEventRank.containsKey(input)) {
                int i = inputEventRank.get(input);
                if(i < 2)
                    inputEventRank.remove(input);
                else
                    inputEventRank.put(input, --i);
            }
        }
        this.eventTypeIds.remove(uuid);
    }

    String findTypetoRellocate(Lettuce store) { // finds the event type that uses the least used input
        String leastUsedInput = inputEventRank.firstKey();
        String leastSimilarEventTypeId = null;
        for( String typeId : eventTypeIds){
            if(store.getEventTypeInputs(typeId).contains(leastUsedInput)){
                leastSimilarEventTypeId = typeId;
                break;
            }
        }
        return leastSimilarEventTypeId;
    }

}
