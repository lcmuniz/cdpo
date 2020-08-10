package worker.Subscriber;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData.Record;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import scala.Predef;
import worker.Connections.Sender;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class Subscriber {
    private String typeId;
    private Sender sender;
    private Schema schema;

    /* The event type is also used as the Routing Key */

    public Subscriber(String TypeId, Sender sender, Schema scheme){
        this.sender = sender;
        this.schema = scheme;
        this.typeId = TypeId;
    }

    public void update(Map row) {
        Record eventDetected = fromMap(row,schema);
        System.out.print(schema.getName()+" : "+eventDetected.toString()+"\n");
        sender.Publish(typeId, eventDetected);
    }

    private static Record fromMap(Map row, Schema schema){
        Record event = new Record(schema);
        boolean subfield = true;
        String firstKey = (String) row.keySet().toArray()[0];
        for(Schema.Field field : schema.getFields()){
            if(field.name().equals(firstKey)){
                subfield = false;
                for (Object i : row.keySet()) {
                    event.put((String.valueOf(i)), row.get(i));
                }
                break;
            }
        }

        if(subfield){
            event = (Record) row.get(firstKey);
        }
        return event;
    }

}