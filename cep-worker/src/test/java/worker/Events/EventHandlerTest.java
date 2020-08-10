package worker.Events;

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import com.rabbitmq.client.*;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.reflect.AvroSchema;
import worker.Connections.RabbitmqSender;
import worker.utils.Serializer;
import worker.utils.SerializerTest;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class EventHandlerTest extends TestCase{

    public void testHandle() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender  = new RabbitmqSender(channel);
        EventHandler eventHandler = new EventHandler();
        String query1 = "SELECT * FROM MyAvroEvent as VanLocation WHERE carType = \"Van\" ";
        String query2 = "SELECT carType,carId FROM MyAvroEvent as Location WHERE carId = 7 ";
        String fields = "carId int, carType String";

        eventHandler.addInputStream("1","MyAvroEvent",SerializerTest.generateSchema());
        eventHandler.addCheckExpression("2","VanLocation",query1,sender);
        eventHandler.addCheckExpression("3","Location",query2, sender);

        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(SerializerTest.generateEvent(),"MyAvroEvent");

        eventHandler.handle(SerializerTest.generateEventTruck(),"MyAvroEvent");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                assertEquals(SerializerTest.generateEvent(), Serializer.AvroDeserialize(body,SerializerTest.generateSchema()));
            }
        };
        channel.basicConsume(queueName, true, consumer);
        eventHandler.deleteCheckExpression("3");
        eventHandler.deleteCheckExpression("2");
        eventHandler.deleteInputStream("1");
        channel.close();
        connection.close();
    }

    public void testBusEventHandle() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender  = new RabbitmqSender(channel);
        EventHandler eventHandler = new EventHandler();
        String query1 = "SELECT * FROM bus718 WHERE c = \"Truck\" ";

        eventHandler.addInputStream("718","bus718",generateBusEventSchema());
        eventHandler.addCheckExpression("B2","SaintMichel",query1,sender);

        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(genBusEvt1(),"bus718");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                assertEquals(SerializerTest.generateEvent(), Serializer.AvroDeserialize(body,SerializerTest.generateSchema()));
            }
        };
        channel.basicConsume(queueName, true, consumer);
        eventHandler.deleteCheckExpression("B2");
        eventHandler.deleteInputStream("718");
        channel.close();
        connection.close();
    }

    public void testAggregation() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender  = new RabbitmqSender(channel);
        EventHandler eventHandler = new EventHandler();
        String query = "SELECT average from MyAvroEvent#length(4)#uni(carId)";

        eventHandler.addInputStream("1","MyAvroEvent",SerializerTest.generateSchema());
        eventHandler.addCheckExpression("2","media",query,sender);

        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(SerializerTest.generateEvent(),"MyAvroEvent");
        eventHandler.handle(SerializerTest.generateEventTruck(),"MyAvroEvent");
        eventHandler.handle(SerializerTest.generateEvent(),"MyAvroEvent");
        eventHandler.handle(SerializerTest.generateEventTruck(),"MyAvroEvent");

        Schema schema = eventHandler.getSchema("2");
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {

                assertEquals(schema.getFields(),Serializer.AvroDeserialize(body,schema).getSchema().getFields());
            }
        };
        channel.basicConsume(queueName, true, consumer);
        eventHandler.deleteCheckExpression("2");
        eventHandler.deleteInputStream("1");
        channel.close();
        connection.close();

    }

    public void testHandleWithTimeStamp() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender  = new RabbitmqSender(channel);
        EventHandler eventHandler = new EventHandler();
        String query1 = "SELECT * FROM MyAvroEvent#ext_timed(timestamp, 1) as VanLocation WHERE carType = \"Truck\" ";
        String query2 = "SELECT carType,carId FROM MyAvroEvent#ext_timed(timestamp, 1) as Location WHERE carId = 7 ";

        eventHandler.addInputStream("1","MyAvroEvent",generateTimestampSchema());
        eventHandler.addCheckExpression("2","VanLocation",query1,sender);
        eventHandler.addCheckExpression("3","Location",query2, sender);

        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(generateTimestampEvent1(),"MyAvroEvent");
        eventHandler.handle(generateTimestampEvent2(),"MyAvroEvent");
        eventHandler.handle(generateTimestampEvent3(),"MyAvroEvent");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                assertEquals(SerializerTest.generateEvent(), Serializer.AvroDeserialize(body,SerializerTest.generateSchema()));
            }
        };
        channel.basicConsume(queueName, true, consumer);
        eventHandler.deleteCheckExpression("3");
        eventHandler.deleteCheckExpression("2");
        eventHandler.deleteInputStream("1");
        channel.close();
        connection.close();
    }

    public void testAggregationWithTimestamp() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender  = new RabbitmqSender(channel);
        EventHandler eventHandler = new EventHandler();
        String query = "SELECT average from MyAvroEvent#length(4)#uni(carId)";

        eventHandler.addInputStream("1","MyAvroEvent",generateTimestampSchema());
        eventHandler.addCheckExpression("2","avgwithT",query,sender);

        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(generateTimestampEvent1(),"MyAvroEvent");
        eventHandler.handle(generateTimestampEvent2(),"MyAvroEvent");
        eventHandler.handle(generateTimestampEvent3(),"MyAvroEvent");

        Schema schema = eventHandler.getSchema("2");


        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {

                assertEquals(schema.getFields(),Serializer.AvroDeserialize(body,schema).getSchema().getFields());
            }
        };
        channel.basicConsume(queueName, true, consumer);
        eventHandler.deleteCheckExpression("2");
        eventHandler.deleteInputStream("1");
        channel.close();
        connection.close();

    }

    public void testTimeAggregationWithTimestamp() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender  = new RabbitmqSender(channel);
        EventHandler eventHandler = new EventHandler();
        String query = "SELECT avg(carId) as avgcar, timestamp from MyAvroEvent#ext_timed(timestamp,1)";

        eventHandler.addInputStream("1","MyAvroEvent",generateTimestampSchema());
        eventHandler.addCheckExpression("2","avgwithT",query,sender);

        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(generateTimestampEvent1(),"MyAvroEvent");
        eventHandler.handle(generateTimestampEvent2(),"MyAvroEvent");
        eventHandler.handle(generateTimestampEvent3(),"MyAvroEvent");

        Schema schema = eventHandler.getSchema("2");



        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {

                assertEquals(schema.getFields(),Serializer.AvroDeserialize(body,schema).getSchema().getFields());
            }
        };
        channel.basicConsume(queueName, true, consumer);
        eventHandler.deleteCheckExpression("2");
        eventHandler.deleteInputStream("1");
        channel.close();
        connection.close();


        // In this example, it uses the "timestamp" field to track the time of the aggregation.
        // Since the first two events come with just .4 second of diference,
        // the second trigger agregates the average of the carId field.
        // However, the third event comes 6 seconds later,
        // falling outside the 1 second window.

    }

    static Schema generateTimestampSchema(){
        Schema.Parser parser = new Schema.Parser();
        return parser.parse("{" +
                "  \"type\" : \"record\"," +
                "  \"name\" : \"MyAvroEvent\"," +
                "  \"fields\" : [ "+
                "{ \"name\" : \"carId\", \"type\" : \"int\" },"+
                "{ \"name\" : \"carType\", \"type\" : { \"type\" : \"string\", \"avro.java.string\" : \"String\" } }," +
                "{ \"name\" : \"timestamp\", \"type\" : { \"type\" : \"long\", \"logicalType\" : \"timestamp-millis\" } }" +
                " ]" +
                "}");
    }

    static GenericData.Record generateTimestampEvent1(){
        GenericData.Record event;
        event = new GenericData.Record(generateTimestampSchema());
        event.put("carId",6);
        event.put("carType","Truck");
        long date = Long.valueOf("1358080561000");
        event.put("timestamp",date);
        return event;
    }

    static GenericData.Record generateTimestampEvent2(){
        GenericData.Record event;
        event = new GenericData.Record(generateTimestampSchema());
        event.put("carId",7);
        event.put("carType","Truck");
        long date = Long.valueOf("1358080561000");
        event.put("timestamp",date+400);
        return event;
    }

    static GenericData.Record generateTimestampEvent3(){
        GenericData.Record event;
        event = new GenericData.Record(generateTimestampSchema());
        event.put("carId",8);
        event.put("carType","Truck");
        long date = Long.valueOf("1358080561000");
        event.put("timestamp",date+6000);
        return event;
    }

    static GenericData.Record genBusEvt1(){
        GenericData.Record event;
        event = new GenericData.Record(generateBusEventSchema());
        event.put("c","Truck");
        event.put("cl",234);
        event.put("timestamp",System.currentTimeMillis());
        event.put("ta",System.currentTimeMillis());
        event.put("sl", 1);
        event.put("lt0", "letreiro 0");
        event.put("lt1", "letreitro 1");
        event.put("qv", 8);
        event.put("p", 718);
        event.put("a", true);
        event.put("py", 12.0398483);
        event.put("px", 38.09328472);
        return event;
    }

    static Schema generateBusEventSchema(){
        Schema.Parser parser = new Schema.Parser();
        return parser.parse(AvroSchema);
    }

    static String AvroSchema = "{" +
            "  \"type\" : \"record\"," +
            "  \"name\" : \"bus718\"," +
            "  \"fields\" : [ " +
            "{ \"name\" : \"c\",   \"type\" : { \"type\" : \"string\", \"avro.java.string\" : \"String\" } }," +
            "{ \"name\" : \"cl\",  \"type\" : \"int\" }," +
            "{ \"name\" : \"sl\",  \"type\" : \"int\" }," +
            "{ \"name\" : \"lt0\", \"type\" : { \"type\" : \"string\", \"avro.java.string\" : \"String\" } }," +
            "{ \"name\" : \"lt1\", \"type\" : { \"type\" : \"string\", \"avro.java.string\" : \"String\" } }," +
            "{ \"name\" : \"qv\",  \"type\" : \"int\" }," +
            "{ \"name\" : \"p\",   \"type\" : \"int\" }," +
            "{ \"name\" : \"a\",   \"type\" : \"boolean\" }," +
            "{ \"name\" : \"ta\",  \"type\" : { \"type\" : \"long\", \"logicalType\" : \"timestamp-millis\" } }," +
            "{ \"name\" : \"py\",  \"type\" : \"double\" }," +
            "{ \"name\" : \"px\",  \"type\" : \"double\" }," +
            "{ \"name\" : \"timestamp\",  \"type\" : { \"type\" : \"long\", \"logicalType\" : \"timestamp-millis\" } }" +
            "] }";

}
