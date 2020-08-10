package worker.Connections;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.util.concurrent.TimeoutException;


import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import worker.Events.EventHandler;
import worker.utils.Serializer;

import static java.lang.System.exit;

public class RabbitmqReceiver implements Receiver{
    private String queue_name;
    private final String exchange_name = System.getenv("EXCHANGE");
    private Connection connection;
    private Channel channel;
    private final Consumer consumer;
    private int eventsReceivedperPeriod;
    private static Logger LOG = LoggerFactory.getLogger(RabbitmqReceiver.class);

    public RabbitmqReceiver(Schema schema,String TypeId,String TypeName,final EventHandler eventHandler,Connection c) {
        LOG.info("Creating Rabbitmq receiver connection");
        //ConnectionFactory factory = new ConnectionFactory();
        //factory.setHost(System.getenv("RABBITMQ_HOST"));
        //factory.setUsername(System.getenv("RABBITMQ_USERNAME"));
        //factory.setPassword(System.getenv("RABBITMQ_PASSWORD"));
        eventsReceivedperPeriod = 0;
        try {
            connection = c;
            channel = connection.createChannel();
            channel.exchangeDeclare(exchange_name, "topic");
            queue_name = channel.queueDeclare().getQueue();
            channel.queueBind(queue_name, exchange_name, TypeId);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //if(channel.isOpen()) System.out.print("Channel open on creation\n");
        consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                Record event = Serializer.AvroDeserialize(body, schema);
                eventHandler.handle(event, TypeName);
                long deliveryTag = envelope.getDeliveryTag();
//                channel.basicAck(deliveryTag, false);
            }
        };

        try {
            channel.basicConsume(queue_name, true, "myConsumerTag", consumer );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public RabbitmqReceiver(Schema schema, String xg,String TypeId, String TypeName, Channel channel, final EventHandler eventHandler) {
        LOG.info("Creating Rabbitmq receiver connection");

        ;
        try {
            channel.exchangeDeclare(xg, "topic");
            queue_name = channel.queueDeclare().getQueue();
            channel.queueBind(queue_name, xg, TypeId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.channel = channel;
        if(channel.isOpen()) System.out.print("Channel received on creation\n");

        consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                Record event = Serializer.AvroDeserialize(body, schema);
                eventHandler.handle(event, TypeName);
                eventsReceivedperPeriod++;
            }
        };
        try {
            channel.basicConsume(queue_name,true,"12344",false,true,null,consumer);
            //channel.basicConsume(queue_name, true, consumer);
        } catch (IOException | ShutdownSignalException e) {
            e.printStackTrace();
        }
    }


    public void CloseConnection()  {
        if(channel.isOpen()) System.out.print("Channel marked for closing\n");
        try {
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }


    public int eventsPerPeriod(){
        if(channel.isOpen()) System.out.print("Channel open for counting\n");
        int i = eventsReceivedperPeriod;
        eventsReceivedperPeriod = 0;
        return i;
    }

    void Reroute(Consumer consumer) {
        if(channel.isOpen()) System.out.print("Channel open for rerouting\n");
        try {
            channel.basicConsume(queue_name, true, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        eventsReceivedperPeriod++;
    }

}
