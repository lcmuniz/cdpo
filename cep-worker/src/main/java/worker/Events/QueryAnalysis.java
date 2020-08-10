package worker.Events;

import java.util.Map;

public class QueryAnalysis {

    public static boolean hasDataWindow(String query, String name){
        return query.split(name)[1].split(",")[0].contains("#");
    }

    public static String getStateType(String query,String name){
        return query.split(name+"#")[1].split("\\(")[0];
    }

    public static String getStateNumber(String query,String name){
        return query.split(name+"#")[1].split("\\(")[1].split("\\)")[0];
    }

    public static long analyzeQueryTime(String stateType,String stateNumber){
        if(stateType.equalsIgnoreCase("ext_time")){
            return Long.valueOf(stateNumber.split(",")[1]) * 1000;
        }
        else if(stateType.equalsIgnoreCase("ext_time_batch")){
            return 2 * Long.valueOf(stateNumber.split(",")[1]) * 1000;
        }
        else return 0;
    }

    public static void analyzeQueryLengh(String stateType,String stateNumber, String Id, Map counters){
        if(stateType.equalsIgnoreCase("length")){
            counters.put(Id, Integer.valueOf(stateNumber));
        }
        else if(stateType.equalsIgnoreCase("length_batch")) {
            counters.put(Id, 2 * Integer.valueOf(stateNumber));
        }
    }
}
