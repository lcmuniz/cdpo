package worker.Events;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import worker.Connections.Sender;
import worker.Subscriber.Subscriber;
import java.util.concurrent.ConcurrentHashMap;


public class EventHandler {

    private static Logger LOG = LoggerFactory.getLogger(EventHandler.class);
    private EPRuntime epRuntime;
    private CompilerArguments arguments;
    private ConcurrentHashMap<String,Schema> schemas;

    public EventHandler() {
        LOG.info("Initializing Event Handler Service ..");
        Configuration config = new Configuration();
        arguments = new CompilerArguments();
        config.getCommon().addEventTypeAutoName("worker");
        config.getCompiler().getByteCode().setAllowSubscriber(true);
        config.getCompiler().getByteCode().setAccessModifierEventType(NameAccessModifier.PUBLIC);
        config.getCompiler().getByteCode().setBusModifierEventType(EventTypeBusModifier.BUS);
        epRuntime = EPRuntimeProvider.getDefaultRuntime(config);
        arguments.setConfiguration(config);
        schemas = new ConcurrentHashMap<>();
    }

    public void addInputStream(String InputTypeId, String TypeName,Schema scheme){
        String inputStream = "create avro schema "+TypeName+" ( ";
        String[] sch = scheme.toString().split(",");
        for( Object i : scheme.getFields().toArray()){
            String[] si = i.toString().split(" ");
            inputStream = inputStream + si[0]+ " ";
            inputStream = inputStream + si[1].split(":")[1].toLowerCase()+" , ";
        }
        inputStream = inputStream.substring(0,inputStream.length()-3);
        inputStream = inputStream+" ) ";

        System.out.print("New input: "+inputStream+"\n");
        EPCompiled compiled = compile(inputStream,arguments);
        try {
            epRuntime.getDeploymentService().deploy(compiled,new DeploymentOptions().setDeploymentId(InputTypeId));
        } catch (EPDeployException e) {
            e.printStackTrace();
        }
    }

    public void deleteInputStream(String TypeId){
        try {
            epRuntime.getDeploymentService().undeploy(TypeId);
        } catch (EPUndeployException e) {
            e.printStackTrace();
        }
    }

    private void addSchema(String TypeId, Schema scheme){ schemas.put(TypeId,scheme); }

    public Schema getSchema(String TypeId){ return schemas.get(TypeId); }

    private void deleteSchema(String TypeId){ schemas.remove(TypeId); }

    public void addCheckExpression(String TypeId,String TypeName, String Query, Sender sender){
        System.out.print("Adding event type for processing"+TypeName+"\n");
        arguments.getPath().add(epRuntime.getRuntimePath());


        EPCompiled compiled = compileQuery(Query,arguments);
        epRuntime.getRuntimeInstanceWideLock().writeLock().lock();
        try {
            DeploymentOptions options = new DeploymentOptions().setDeploymentId(TypeId);
            EPDeployment deployment = epRuntime.getDeploymentService().deploy(compiled,options);
            Schema schema = (Schema) ((AvroSchemaEventType) deployment.getStatements()[0].getEventType()).getSchema();
            Schema renamedSchema = RenameSchema(TypeName,schema);
            Subscriber subs = new Subscriber(TypeId,sender,renamedSchema);
            deployment.getStatements()[0].setSubscriber(subs);
            addSchema(TypeId,RenameSchema(TypeName,schema));
        } catch (EPDeployException e) {
            e.printStackTrace();
        }
        finally {
            epRuntime.getRuntimeInstanceWideLock().writeLock().unlock();
        }

        System.out.print(TypeName+" Added on EventHandler\n");
    }


    public void deleteCheckExpression(String TypeId) { /*refactor*/
        deleteSchema(TypeId);
        try {
            epRuntime.getDeploymentService().undeploy(TypeId);
        } catch (EPUndeployException e) {
            e.printStackTrace();
        }
    }

    private static EPCompiled compileQuery(String statement,CompilerArguments arguments){
        return compile("@EventRepresentation(avro) "+statement,arguments);
    }

    private static EPCompiled compile(String statement,CompilerArguments arguments ){
        try {
            return EPCompilerProvider.getCompiler().compile(statement, arguments);
        } catch (EPCompileException e) {
            e.printStackTrace();
        }
        return null;
    }


    private static Schema RenameSchema(String TypeName,Schema schema){
        Schema.Parser parser = new Schema.Parser();
        String s = schema.toString();
        //System.out.print("s :"+s+"\n\n");
        String [] s2 = s.split(",");
        //System.out.print("s2[1] :"+s2[1]+"\n\n");
        s2[1] = "\"name\":\""+TypeName+"\"";
        //System.out.print("s2[1] :"+s2[1]+"\n\n");
        s = String.join(",",s2);
        //System.out.print("s :"+s+"\n\n");
        return parser.parse(s);
    }


    public void handle(Record Event,String TypeName) {
        epRuntime.getEventService().sendEventAvro(Event,TypeName);
    }



}
