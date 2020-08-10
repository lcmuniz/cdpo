package worker.DatabaseAccess;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.api.sync.RedisSortedSetCommands;
import org.apache.avro.Schema;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Lettuce {

    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> connection;
    private RedisCommands<String,String> syncCommands;
    private RedisSortedSetCommands<String,String> sortedSetCommands;

    /*public Lettuce(){
        String host = System.getenv("REDIS_HOST");
        redisClient = RedisClient.create("redis://"+host+":6379");
        connection = redisClient.connect();
        syncCommands = connection.sync();
        sortedSetCommands = connection.sync();
    }*/

    public Lettuce(String hostname){
        String host;
        if( hostname == null) {
            host = System.getenv("REDIS_CEP_HOST");
            String pass = System.getenv("REDIS_CEP_PASS");
            RedisURI redisUri = RedisURI.Builder.redis(host)
                    .withPassword(pass)
                    .build();
            redisClient = RedisClient.create(redisUri);
        }
        else {
            host = hostname;
            redisClient = RedisClient.create("redis://" + host + ":6379");
        }
        connection = redisClient.connect();
        syncCommands = connection.sync();
        sortedSetCommands = connection.sync();
    }

    public String getEventTypeName(String id){ return syncCommands.get(id+":Name");  } // from cataloger

    public String getEventTypeDefinition(String id){ return syncCommands.get(id+":Definition");} // from cataloger

    public Schema getAvroSchema(String id){
        String schema = syncCommands.get(id+":AvroSchema");
        Schema.Parser parser = new Schema.Parser();
        return parser.parse(schema);
    }

    public void setAvroSchema(String id,Schema schema){
        syncCommands.set(id+":AvroSchema",schema.toString());
    }

    public String getEventTypeCurrentWorker(String uuid){ return syncCommands.get(uuid+":WorkerId"); }

    public void setEventTypeCurrentWorker(String uuid, String WorkerId){ syncCommands.set(uuid+":WorkerId",WorkerId);}

    public boolean EventTypeCurrentWorker(String uuid, String WorkerId){
        return syncCommands.get(uuid+":WorkerId").equalsIgnoreCase(WorkerId);
    }

    public Set<String> getEventTypeInputs(String id){   // from cataloger
        return syncCommands.smembers(id+":Inputs");
    }

    public void eventTypeStateBuildedAdd(String uuid, String WorkerId){
        syncCommands.sadd(uuid+":StateBuilded",WorkerId);
    }

    public boolean eventTypeStateBuilded(String uuid, String WorkerId){
        return syncCommands.sismember(uuid+":StateBuilded",WorkerId);
    }

    public void eventTypeStateBuildedDel(String uuid, String WorkerId){
        syncCommands.srem(uuid+":StateBuilded",WorkerId);
    }

    public void eventTypeAssigned(String uuid){
        syncCommands.srem("Unnasigned",uuid);
        syncCommands.sadd("Assigned",uuid);
    }

    public Set<String> getUnnasignedEventTypes(){
        return syncCommands.smembers("Unnasigned");
    }

    public String getUnnasignedEventType(){
        Set<String> Unnasigned = null;
        Unnasigned = syncCommands.smembers("Unnasigned");
        return Unnasigned.iterator().next();
    }

    public boolean hasUnnasignedEvents(){
        long UnnasignedTypes = syncCommands.scard("Unnasigned");
        return UnnasignedTypes > 0;
    }

    public void pushEventTypeForRellocation(String uuid,Double score){
        syncCommands.zadd("RellocationStack",score,uuid);
    }

    public boolean hasEventTypeBeenPickedUpForRellocation(String uuid){
        return syncCommands.zscore("RellocationStack", uuid) != null;
    }

    public String popEventTypeForAcceptingRellocation(){
        //return sortedSetCommands.zrevrange("RellocationStack",0,1).iterator().next();
        return sortedSetCommands.zpopmax("RellocationStack").toString();
    }

    public String getEventTypeReceiveingWorker(String uuid){
        return syncCommands.get(uuid+":ReceveingWorkerId");
    }

    public void removeEventTypeReceveingWorker(String uuid){
        syncCommands.del(uuid+":ReceveingWorkerId");
    }

    public void setEventTypeReceveingWorker(String uuid, String WorkerId){
        syncCommands.set(uuid+":ReceveingWorkerId",WorkerId);
    }

    public Set<String> getDeletedEventTypes(Set<String> currentEventTypes, String Workerid){
        HashSet<String> deletedIds = new HashSet<>();
        Set<String> registeredIds = syncCommands.smembers("Registered");
        for(String TypeId : currentEventTypes){
            if(!registeredIds.contains(TypeId)){
                deletedIds.add(TypeId);
            }
        }
        return deletedIds;
    }


    public void Close(){
        connection.close();
        redisClient.shutdown();

    }

}
