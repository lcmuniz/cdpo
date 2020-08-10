package worker.Connections;

import io.nats.client.Connection;
import io.nats.client.Nats;
import org.apache.avro.generic.GenericData;
import worker.utils.Serializer;

import java.io.IOException;

public class NATSSender implements Sender {

    private Connection nc;
    private boolean stopMessages;

    public NATSSender(){
        String NatsHost = System.getenv("NATS_HOST");
        String NatsPort = System.getenv("NATS_PORT");
        try {
            nc = Nats.connect("nats://"+NatsHost+":"+NatsPort);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        stopMessages = false;
    }

    public void stopSending(){stopMessages = true; }

    public void restartSending(){ stopMessages = false;}

    public void CloseConnection(){
        try {
            nc.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void Publish(String routingKey, GenericData.Record event){
        if(!stopMessages) send(routingKey,event);
    }

    private void send(String subject, GenericData.Record event){
        byte [] message = Serializer.AvroSerialize(event);
        nc.publish(subject,message);
    }
}
