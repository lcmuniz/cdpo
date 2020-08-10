package worker.Connections;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.avro.generic.GenericData.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import worker.utils.Serializer;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitmqSender implements Sender{
    private final String exchange_name;
    private Connection connection;
    private Channel channel;
    private boolean stopMessages;


    private static Logger LOG = LoggerFactory.getLogger(RabbitmqSender.class);

    public RabbitmqSender(Connection c) {
        LOG.info("Creating Rabbitmq sender connection");
        //ConnectionFactory factory = new ConnectionFactory();
        //factory.setHost(System.getenv("RABBITMQ_HOST"));
        exchange_name = System.getenv("EXCHANGE");
        //factory.setUsername(System.getenv("RABBITMQ_USERNAME"));
        //factory.setPassword(System.getenv("RABBITMQ_PASSWORD"));
        stopMessages = false;
        try {
            connection = c;
            channel = connection.createChannel();
            channel.exchangeDeclare(exchange_name, "topic");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public RabbitmqSender(Channel channel){
        exchange_name = "EXCHANGE";
        stopMessages = false;
        try {
            channel.exchangeDeclare(exchange_name, "topic");
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.channel = channel;

    }

    public void stopSending(){
        stopMessages = true;
    }

    public void restartSending(){
        stopMessages = false;
    }




    public void CloseConnection() {
        try {
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }

    }

    public void Publish(String routingKey,Record event)  {
        if(!stopMessages) {
            LOG.info("sending message");
            send(routingKey, event);
        }

    }


    private void send(String routingKey,Record event)  {
        byte [] message = Serializer.AvroSerialize(event);
        try {
            channel.basicPublish(exchange_name, routingKey, null, message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}