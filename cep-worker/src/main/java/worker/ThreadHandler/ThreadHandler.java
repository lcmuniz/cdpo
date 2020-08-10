package worker.ThreadHandler;

//import com.rabbitmq.client.Channel;
//import com.rabbitmq.client.Connection;
//import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.avro.Schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import worker.ClusterHandler.ClusterHandler;
import worker.ClusterHandler.KubernetesHandler;
import worker.Connections.*;
import worker.DatabaseAccess.Lettuce;
import worker.Events.EventHandler;
import worker.Events.QueryAnalysis;
import worker.LoadBalancers.InputSimilarity;
import worker.LoadBalancers.LoadBalancer;
import worker.ResourcesAnalysis.ResourceAnalysis;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.*;

import static java.lang.System.exit;

public class ThreadHandler {
    private static Logger LOG = LoggerFactory.getLogger(ThreadHandler.class);

    private static String Broker = System.getenv("BROKER");

    private ClusterHandler workerHandler;

    private volatile EventHandler eventHandler;

    private ExecutorService executor;
    private final String WorkerId;
    private volatile LoadBalancer loadBalancer;

    private ConcurrentSkipListMap<String, Receiver> ReceiverList;
    private ConcurrentSkipListMap<String, Sender> SenderList;

    private ConcurrentSkipListMap<String,Integer> counters;

    //private RabbitmqSender sender;

    private String RedisHost;

    private Connection conn;



    public ThreadHandler(String WorkerID,String redishost){
        LOG.debug("Creating Event Handler\n"); // Translate EPL statements into triggers
        this.eventHandler = new EventHandler();
        LOG.debug("Creating Event Sender\n "); // Class for sending events to RabbitMQ

        this.executor = Executors.newCachedThreadPool();

        LOG.debug("Creating Event Receivers\n"); // Class for receiving events from other instances
        this.ReceiverList = new ConcurrentSkipListMap<>();
        this.SenderList = new ConcurrentSkipListMap<>();


        this.WorkerId = WorkerID;

        this.RedisHost = redishost;


        this.loadBalancer = new InputSimilarity(RedisHost);

        this.workerHandler = new KubernetesHandler();

        //Class for storing counters for rellocation purposes
        this.counters = new ConcurrentSkipListMap<>();

        System.out.print("Chosen Broker: "+Broker+"\n");
        if(("Rabbitmq").equals(Broker)) {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(System.getenv("RABBITMQ_HOST"));
            factory.setUsername(System.getenv("RABBITMQ_USERNAME"));
            factory.setPassword(System.getenv("RABBITMQ_PASSWORD"));
            try {
                conn = factory.newConnection();
                System.out.print("Conn in open: "+conn.isOpen()+"\n");
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        }

    }

    public void ContinuousSingleProcessing(){
        System.out.print("Single processing starting\n");

        Lettuce store = new Lettuce(RedisHost);
        System.out.print("Begin Event Type Assignment\n");
        boolean status = true;
        while (status) {
            if (store.hasUnnasignedEvents()) {
                Set<String> EventTypeIds = store.getUnnasignedEventTypes();
                System.out.print("Adding event types in thread handler\n");
                for (String EventTypeId : EventTypeIds) {
                    System.out.print("Adding event typeId:"+EventTypeId+"\n");
                    AddOutgoingEventType(EventTypeId,store);
                }

            }

            try {
                TimeUnit.MINUTES.sleep(15);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    public void InitiateAcceptionOfEvents(){
        System.out.print("Worker:"+WorkerId+" starting\n");
        executor.submit(() -> {
            LOG.info("Accepting event types");
            boolean status = true;
            while (status) {
                Lettuce store = new Lettuce(RedisHost);
                while(!ResourceAnalysis.Overload() && !ResourceAnalysis.Underload()){
                    String EventTypetoAcceptRealocation = store.popEventTypeForAcceptingRellocation();
                    if(!EventTypetoAcceptRealocation.isEmpty())
                        AcceptRelocation(EventTypetoAcceptRealocation);
                    else
                        AddUnnasignedEventType(store.getUnnasignedEventType(),store);
                }
                store.Close();
                try {
                    Thread.sleep(Integer.parseInt(System.getenv("TIMEOUT_A")));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    status = false;
                }
            }

        });
    }

    public void InitiateUnderloadMonitoring(){
        executor.submit(() -> {
            boolean status = true;
            while (status) {
                Lettuce store = new Lettuce(RedisHost);
                Set<String> deletedEventTypes = store.getDeletedEventTypes(SenderList.keySet(),WorkerId);
                for(String deletedTypeId : deletedEventTypes){
                    deleteCheckExpressionAndUnnecessaryInputs(store,deletedTypeId);
                }
                store.Close();
                try {
                    Thread.sleep(Integer.parseInt(System.getenv("TIMEOUT_U")));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    status = false;
                }
                while(ResourceAnalysis.Underload()) {
                    if(ReceiverList.isEmpty()){
                        workerHandler.StopAndRemoveWorkerFromCluster(WorkerId);
                    }
                    String uuid = loadBalancer.findEventTypetoRellocate();
                    Relocate(ResourceAnalysis.getResourceUsage(),uuid);
                }
            }
        });
    }

    public void InitiateOverloadMonitoring(){
        executor.submit(() -> {
            boolean status = true;
            while (status) {
                try {
                    Thread.sleep(Integer.parseInt(System.getenv("TIMEOUT_O")));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    status = false;
                }
                Lettuce store = new Lettuce(RedisHost);
                Set<String> deletedEventTypes = store.getDeletedEventTypes(SenderList.keySet(),WorkerId);
                for(String deletedTypeId : deletedEventTypes){
                    deleteCheckExpressionAndUnnecessaryInputs(store,deletedTypeId);
                }
                while (ResourceAnalysis.Overload()) {
                    String typeToRellocate = loadBalancer.findEventTypetoRellocate();
                    boolean newWorkerInstantiaded = false;
                    boolean RealocationStarted = false;
                    for(int i = 0 ; i < 3 && !RealocationStarted ; i++)
                         RealocationStarted = Relocate(ResourceAnalysis.getResourceUsage(), typeToRellocate);

                    while(!RealocationStarted){
                        newWorkerInstantiaded = workerHandler.instantiateNewWorker();
                        if(newWorkerInstantiaded) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        RealocationStarted = Relocate(ResourceAnalysis.getResourceUsage(), typeToRellocate);
                    }
                }
                store.Close();
            }
        });
    }

    //private void AcceptRellocation(String OutputTypeID){ AcceptRelocation(OutputTypeID); }



    private void AcceptRelocation(String OutputTypeID){

        Lettuce store = new Lettuce(RedisHost);

        String SendingWorkerID = setUpReceivingAndSendingWorkerIds(store,OutputTypeID);

        addEventTypeToThreadHandler(OutputTypeID);

        addEventTypeToRuntime(store,OutputTypeID);

        //Evaluating time and events needed to build state
        long timeToWait = evaluateInputsToBuildState(store,OutputTypeID);

        //building state
        buildStateAndSave(store,OutputTypeID,timeToWait);

        //waiting for other worker to stop sending events
        waitSendingWorkerNotification(OutputTypeID,SendingWorkerID,store);

        //reset current worker in db
        startSendingEvents(OutputTypeID);

        //update load balancer rank
        loadBalancer.addEventTypeToRank(OutputTypeID,ReceiverList);

        store.Close();
    }

    private String setUpReceivingAndSendingWorkerIds(Lettuce store, String OutputTypeID){
        store.setEventTypeReceveingWorker(OutputTypeID,WorkerId);

        return store.getEventTypeCurrentWorker(OutputTypeID);
    }
    private void addEventTypeToThreadHandler(String OutputTypeID){
        //Creating new Sender and stopping from sending events

        Sender sender = NewSender();
        sender.stopSending();
        SenderList.put(OutputTypeID,sender);

    }
    private void addEventTypeToRuntime(Lettuce store, String OutputTypeID){
        String Name = store.getEventTypeName(OutputTypeID);
        String Query = store.getEventTypeDefinition(OutputTypeID);
        eventHandler.addCheckExpression(OutputTypeID,Name, Query, SenderList.get(OutputTypeID));
        store.setAvroSchema(OutputTypeID,eventHandler.getSchema(OutputTypeID));
    }
    private long evaluateInputsToBuildState(Lettuce store,String OutputTypeID){
        Set<String> neoIncomingIds;
        String Query = store.getEventTypeDefinition(OutputTypeID);
        long timeToWait = 0;
        neoIncomingIds = store.getEventTypeInputs(OutputTypeID);
        for(String id : neoIncomingIds){
            if(!ReceiverList.containsKey(id)){
                CreateInputReceiver(id);
            }
            String name = store.getEventTypeName(id);
            if(QueryAnalysis.hasDataWindow(Query,name)) {
                String stype = QueryAnalysis.getStateType(Query, name);
                String snumber = QueryAnalysis.getStateNumber(Query, name);
                QueryAnalysis.analyzeQueryLengh(stype, snumber, id, counters);
                //only accept value in seconds ( e.g.(3))
                timeToWait += QueryAnalysis.analyzeQueryTime(stype, snumber);
            }
        }
        return timeToWait;
    }
    private void buildStateAndSave(Lettuce store, String OutputTypeID,long timeToWait){
        boolean countersRemoved = false;
        boolean timePassed = false;
        while(!countersRemoved && !timePassed){ //waiting for state to build
            if(counters.isEmpty()) {
                countersRemoved = true;
            }
            try {
                TimeUnit.SECONDS.sleep(timeToWait);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            timePassed = true;
        }
        store.eventTypeStateBuildedAdd(OutputTypeID,WorkerId);

    }
    private void waitSendingWorkerNotification(String OutputTypeID,String SendingWorkerID,Lettuce store){
        boolean transferConfirmed = false;
        while(!transferConfirmed){
            if(!store.EventTypeCurrentWorker(OutputTypeID,SendingWorkerID))
                transferConfirmed = true;
            try {
                TimeUnit.MILLISECONDS.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    private void startSendingEvents(String OutputTypeID){
        SenderList.get(OutputTypeID).restartSending();
    }

    private boolean Relocate(double resourceusage, String EventTypeId){

        Lettuce store = new Lettuce(RedisHost);

        putEventForRelocation(store,resourceusage,EventTypeId);

        boolean success = waitForEventToBePickUpForRelocation(store,EventTypeId);

        if(!success) return false;

        waitForStateToBeRebuildedElseware(store,EventTypeId);

        acknolegeTransferConfirmed(store,EventTypeId);

        deleteCheckExpressionAndUnnecessaryInputs(store,EventTypeId);

        store.Close();

        //update load balancer rank
        loadBalancer.deleteEventTypeofRank(EventTypeId,ReceiverList);

        return true;

    }

    private void putEventForRelocation(Lettuce lettuce, double resourceusage, String EventTypeId){
        lettuce.pushEventTypeForRellocation(EventTypeId, resourceusage);
    }
    private boolean waitForEventToBePickUpForRelocation(Lettuce lettuce, String EventTypeId){

        try {
            TimeUnit.MILLISECONDS.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return lettuce.hasEventTypeBeenPickedUpForRellocation(EventTypeId);
    }
    private void waitForStateToBeRebuildedElseware(Lettuce store, String EventTypeId){
        String ReceveingWorkerID = store.getEventTypeReceiveingWorker(EventTypeId);
        boolean stateRebuiledElsewhere = store.eventTypeStateBuilded(EventTypeId,ReceveingWorkerID);
        while(!stateRebuiledElsewhere) {
            try {
                TimeUnit.MILLISECONDS.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stateRebuiledElsewhere = store.eventTypeStateBuilded(EventTypeId,ReceveingWorkerID);
        }
    }
    private void acknolegeTransferConfirmed(Lettuce store, String OutputTypeID){
        String newWorker = store.getEventTypeReceiveingWorker(OutputTypeID);
        store.setEventTypeCurrentWorker(OutputTypeID,newWorker);
        store.removeEventTypeReceveingWorker(OutputTypeID);
    }
    private void deleteCheckExpressionAndUnnecessaryInputs(Lettuce store,String EventTypeId){
        //stop sending events
        eventHandler.deleteCheckExpression(EventTypeId);

        //remove unnecessary input event types
        Set<String> EventTypeInputs = store.getEventTypeInputs(EventTypeId);
        Set<String> OtherEventTypes = ReceiverList.keySet();


        for (String input : EventTypeInputs){
            boolean isUsedbyAnotherEventType = false;
            for(String otherId : OtherEventTypes){
                if(store.getEventTypeInputs(otherId).contains(input))
                    isUsedbyAnotherEventType = true;
            }
            if(!isUsedbyAnotherEventType) {
                eventHandler.deleteInputStream(input);
                ReceiverList.get(input).CloseConnection();
                ReceiverList.remove(input);
            }
        }
        SenderList.remove(EventTypeId);
    }

    private void AddUnnasignedEventType(String EventTypeId, Lettuce store){
        store.eventTypeAssigned(EventTypeId);
        System.out.print("Storing Event Type Id : "+EventTypeId+" as assigned\n");
        AddOutgoingEventType(EventTypeId,store);
    }


    private void AddOutgoingEventType(String OutputTypeID,Lettuce store){
        System.out.print("Adding Outgoing EventId : "+OutputTypeID+"\n");
        Set<String> inputTypes = store.getEventTypeInputs(OutputTypeID);
        for(String inputTypeID : inputTypes){
            if(!ReceiverList.containsKey(inputTypeID)){
                System.out.print("Adding Input EventId : "+inputTypeID+"\n");
                CreateInputReceiver(inputTypeID);
            }
        }
        String Name = store.getEventTypeName(OutputTypeID);
        String Query = store.getEventTypeDefinition(OutputTypeID);

        SenderList.put(OutputTypeID,NewSender());
        System.out.print("Creating Sender for Event Type Id : "+OutputTypeID+", name : "+Name+"\n");
        eventHandler.addCheckExpression(OutputTypeID,Name, Query, SenderList.get(OutputTypeID));
        System.out.print("Including Event Type Id : "+OutputTypeID+" on EventHandler\n");
        store.setAvroSchema(OutputTypeID,eventHandler.getSchema(OutputTypeID));
        System.out.print("Storing Event Type Id : "+OutputTypeID+" current Avro Schema\n");
        store.setEventTypeCurrentWorker(OutputTypeID,WorkerId);
        System.out.print("Storing Event Type Id : "+OutputTypeID+" current Worker\n");


        //update load balancer rank

        loadBalancer.addEventTypeToRank(OutputTypeID,ReceiverList);
        System.out.print("Event Type "+Name+" is being detected\n");
    }

    private void CreateInputReceiver(String inputTypeID){
        Lettuce store = new Lettuce(RedisHost);
        String TypeName = store.getEventTypeName(inputTypeID);
        System.out.print("Adding input type Name:"+TypeName+", Id :"+inputTypeID+" \n");
        Schema schema = store.getAvroSchema(inputTypeID);
        store.Close();
        eventHandler.addInputStream(inputTypeID,TypeName,schema);
        ReceiverList.put(inputTypeID,NewReceiver(TypeName,schema,inputTypeID,eventHandler));
    }



    private Sender NewSender(){
        Sender sender;
        if(Broker.equals("Rabbitmq")) sender = new RabbitmqSender(conn);
        else sender = new NATSSender();
        return sender;
    }
    private Receiver NewReceiver(String TypeName, Schema schema, String TypeId, EventHandler eventHandler){
        Receiver receiver;
        if(Broker.equals("Rabbitmq")) receiver = new RabbitmqReceiver(schema,TypeId,TypeName,eventHandler,conn);
        else receiver = new NATSReceiver(TypeName,schema,TypeId,eventHandler);
        return receiver;
    }

}
