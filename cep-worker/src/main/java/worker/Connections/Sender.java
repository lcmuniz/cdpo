package worker.Connections;

import org.apache.avro.generic.GenericData;

public interface Sender {

    void stopSending();

    void restartSending();

    void CloseConnection();

    void Publish(String routingKey, GenericData.Record event);

}
