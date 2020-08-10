package worker.utils;

import com.twitter.bijection.Injection;
import com.twitter.bijection.avro.GenericAvroCodecs;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData.Record;


public class Serializer {

    public static byte[] AvroSerialize(Record Event){
        Injection<Record, byte[]> recordInjection;
        recordInjection = GenericAvroCodecs.toBinary(Event.getSchema());
        return recordInjection.apply(Event);
    }

    public static Record AvroDeserialize(byte [] bytes,Schema schema){
        Injection<Record, byte[]> recordInjection;
        recordInjection = GenericAvroCodecs.toBinary(schema);
        return recordInjection.invert(bytes).get();
    }

    public static Record generateEvent(){
        Record event;
        event = new Record(generateSchema());
        event.put("carId",1);
        event.put("carType","Van");
        return event;
    }

    public static Schema generateSchema(){
        Schema.Parser parser = new Schema.Parser();
        return parser.parse("{" +
                "  \"type\" : \"record\"," +
                "  \"name\" : \"MyAvroEvent\"," +
                "  \"fields\" : [ {" +
                "    \"name\" : \"carId\"," +
                "    \"type\" : \"int\"" +
                "  }, {" +
                "    \"name\" : \"carType\"," +
                "    \"type\" : {" +
                "      \"type\" : \"string\"," +
                "      \"avro.java.string\" : \"String\"" +
                "    }" +
                "  } ]" +
                "}");
    }

}
