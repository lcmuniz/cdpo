package worker.Connections;

import io.nats.client.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import worker.Events.EventHandler;
import worker.utils.Serializer;

import java.io.IOException;
import java.time.Duration;

public class NATSReceiver implements Receiver {

    private Connection nc;
    private int eventsReceivedperPeriod;
    private Dispatcher d;
    private String TypeName;
    private String subject;
    private Schema schema;
    private EventHandler eventHandler;
    private Subscription sub;

    public NATSReceiver(String TypeName, Schema schema, String bindingKey, EventHandler eventHandler){
        String NatsHost = System.getenv("NATS_HOST");
        String NatsPort = System.getenv("NATS_PORT");
        try {
            nc = Nats.connect("nats://"+NatsHost+":"+NatsPort);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        eventsReceivedperPeriod = 0;
        subject = bindingKey;
        this.schema = schema;
        this.eventHandler = eventHandler;
        this.TypeName = TypeName;
        d = nc.createDispatcher((msg) -> {
            GenericData.Record event = Serializer.AvroDeserialize(msg.getData(), schema);
            //System.out.print("Evento chegou : \n"+schema+"\n"+event.getSchema().toString()+"\n"+event.toString()+"\n");
            eventHandler.handle(event, TypeName);
            if(null!=msg.getData()) eventsReceivedperPeriod++;
        });
        d.subscribe(subject);
    }

    public void CloseConnection(){
        try {
            nc.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int eventsPerPeriod(){
        int i = eventsReceivedperPeriod;
        eventsReceivedperPeriod = 0;
        return i;
    }
}
