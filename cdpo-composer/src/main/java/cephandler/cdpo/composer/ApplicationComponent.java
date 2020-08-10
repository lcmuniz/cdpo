package cephandler.cdpo.composer;

import cephandler.cdpo.composer.utils.EventFields;
import cephandler.database.Store;
import cephandler.exception.InvalidParameterException;
import cephandler.exception.MalformatedSchemaException;
import cephandler.exception.TypeNotSupportedException;
import cephandler.helper.CepEventHelper;
import cephandler.utils.Types;
import com.fasterxml.uuid.Generators;
import org.apache.avro.Schema;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;

import javax.swing.*;
import javax.swing.plaf.multi.MultiViewportUI;
import java.util.*;

@RestController
public class ApplicationComponent {
    private static final String REDIS_COMPOSER_HOST = System.getenv("REDIS_COMPOSER_HOST");
    private static final String REDIS_WORKER_HOST = System.getenv("REDIS_WORKER_HOST");
    private CepEventHelper cepEventHelper;



    private List<String> nEventRequiredFields;
    private List<String> rulesRequiredFiels;


    public ApplicationComponent(){
        cepEventHelper = new CepEventHelper();
        cepEventHelper.startConnection();

        generateNewEventRequiredFields();
        generateRulesRequiredFields();
    }

    private void generateRulesRequiredFields() {
        rulesRequiredFiels = new ArrayList<>();
        rulesRequiredFiels.add(EventFields.RULE_UUID_FIELD);
        rulesRequiredFiels.add(EventFields.RULE_NAME_FIELD);
        rulesRequiredFiels.add(EventFields.RULE_DEFINITION_FIELD);
        rulesRequiredFiels.add(EventFields.RULE_DESCRIPTION_FIELD);
        rulesRequiredFiels.add(EventFields.RULE_LEVEL_FIELD);
        rulesRequiredFiels.add(EventFields.RULE_QOS_FIELD);
        rulesRequiredFiels.add(EventFields.RULE_TAGFILTER_FIELD);
        rulesRequiredFiels.add(EventFields.RULE_TARGET_FIELD);
        rulesRequiredFiels.add(EventFields.RULE_INPUTS_FIELD);
    }

    private void generateNewEventRequiredFields() {
        nEventRequiredFields = new ArrayList<>();
        nEventRequiredFields.add(EventFields.EVENT_NAME_FIELD);
        nEventRequiredFields.add(EventFields.EVENT_SPEC_FIELD);
    }

//    @RequestMapping(value = "/send", method = RequestMethod.POST)
//    public void send(@RequestBody String order) {
//        System.out.println("Post sending contexta data...");
//    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String checkHost(){
        return "{ \"202\": \"You are connected to Cdpo-Composer!\" }";
    }



    @RequestMapping(value = "/cdpo/registerNewCdpoEvent", method = RequestMethod.POST)
    public String RegisterCdpoEvent(@RequestBody String body) {
        System.out.println(" body: "+body);

        JSONObject jsonBody = checkRequestBody(body,nEventRequiredFields);

        String eventName = jsonBody.getString(EventFields.EVENT_NAME_FIELD);
        JSONObject specFields = jsonBody.getJSONObject(EventFields.EVENT_SPEC_FIELD);

        /*take AvroSchema fields*/
        List<String> fields = new ArrayList<>();
        List<String> fieldsType = new ArrayList<>();
        Iterator<String> fKeys = specFields.keys();
        while (fKeys.hasNext()){
            String fK = fKeys.next();
            String type = specFields.getString(fK);
            fields.add(fK);
            fieldsType.add(type);
        }

        try {
            /* check or generate uuid */
            String uuid = UUID.randomUUID().toString();;
            if(jsonBody.has(EventFields.EVENT_UUID_FIELD)) {
                Object eUuid = jsonBody.get(EventFields.EVENT_UUID_FIELD);
                if (!eUuid.equals(JSONObject.NULL) && eUuid.toString().compareTo("") != 0) {
                    uuid = eUuid.toString();
                }
            }
            jsonBody.put(EventFields.EVENT_UUID_FIELD, uuid);


            /* store the schema*/
            Schema avroSchema = CepEventHelper.generateAvroSchema(eventName, fields,fieldsType);
            Store store = new Store(REDIS_COMPOSER_HOST);
            store.setEventTypeName(uuid, eventName);
            store.setAvroSchema(uuid, avroSchema);
            store.Close();

            System.out.println("Store Avro Schema: \n"+avroSchema.toString()+" \nwith UUID: "+uuid);
        } catch (MalformatedSchemaException e) {
            e.printStackTrace();
        }

        return jsonBody.toString();
    }


    @RequestMapping(value = "/cdpo/publishNewCdpoEvent/{uuid}", method = RequestMethod.POST)
    public String PublishNewCdpoEvent(@PathVariable("uuid") String uuid, @RequestBody String body) throws InvalidParameterException, MalformatedSchemaException {
        System.out.println("Event UUID: "+ uuid+"\nbody: "+body);
        Store store = new Store(REDIS_COMPOSER_HOST);

        /*Take avroschema*/
        Schema schema =  store.getAvroSchema(uuid);

        /* check body request and take the values*/
        JSONObject jsonObject = new JSONObject(body);
        Set<String> keys = jsonObject.keySet();
        List<Schema.Field> schFields =  schema.getFields();
        List<String> eventFields = new ArrayList<>();
        List<Object> eventValues = new ArrayList<>();
        for(Schema.Field field : schFields){
            if(!keys.contains(field.name()) && field.name().compareTo("r_uuid") != 0){ /* verifica se o field qualquer do esquema nao esta no post*/
                throw new JSONException("Request attribute is required: " + field.name());
            }
            if(field.name().compareTo("r_uuid") != 0) {
                eventFields.add(field.name());
                eventValues.add(jsonObject.get(field.name()));
            }
        }

        String eventName = store.getEventTypeName(uuid);
        cepEventHelper.sendCepEvent("", uuid , eventValues, eventFields, schema);
        store.Close();

        return "Success";
    }


    @RequestMapping(value = "/cdpo/registerNewCdpoRules", method = RequestMethod.POST)
    public String RegisterCdpoRules(@RequestBody String body) {
        System.out.println(" body: "+body);
        Jedis j = new Jedis(REDIS_WORKER_HOST);
        Store store = new Store(REDIS_COMPOSER_HOST);

        JSONObject jsonBody = checkRequestBody(body,rulesRequiredFiels);

        /*obter a lista dos inputs de entrada*/
        if(!jsonBody.has(EventFields.RULE_INPUTS_FIELD)){
            throw new JSONException("Request attribute is required: " + EventFields.RULE_INPUTS_FIELD);
        }
        JSONArray inputsArray = jsonBody.getJSONArray(EventFields.RULE_INPUTS_FIELD);
        Iterator iterator = inputsArray.iterator(); int i =0;
        String[] inputs = new String[inputsArray.length()];
        while (iterator.hasNext()){
            String inputUuid = (String) iterator.next();
            String inputName = store.getEventTypeName(inputUuid);
            Schema inputAvroSchema = store.getAvroSchema(inputUuid);

            j.set(inputUuid+":Name",inputName);
            j.set(inputUuid+":AvroSchema",inputAvroSchema.toString());
            j.sadd("Primitives",inputUuid);

            inputs[i++] = inputUuid;
        }

        /* salvar definition no unnasigned  e registered*/
        /* salvar no redis do cep-worker*/
        UUID uuid = Generators.timeBasedGenerator().generate();
        j.sadd("Registered",uuid.toString());
        j.sadd("Unnasigned",uuid.toString());
        j.set(uuid.toString()+":Name",jsonBody.getString(EventFields.RULE_NAME_FIELD));
        j.set(uuid.toString()+":Definition",jsonBody.getString(EventFields.RULE_DEFINITION_FIELD));
        j.set(uuid.toString()+":Description",jsonBody.getString(EventFields.RULE_DESCRIPTION_FIELD));
        j.set(uuid.toString()+":Level",jsonBody.getString(EventFields.RULE_LEVEL_FIELD));
        j.set(uuid.toString()+":Qos",jsonBody.getString(EventFields.RULE_QOS_FIELD));
        j.set(uuid.toString()+":TagFilter",jsonBody.getString(EventFields.RULE_TAGFILTER_FIELD));
        j.set(uuid.toString()+":Target",jsonBody.getString(EventFields.RULE_TARGET_FIELD));
        j.sadd(uuid.toString()+":Inputs", inputs);
        j.close();
        store.Close();;

        return jsonBody.toString();
    }


    private JSONObject checkRequestBody(String body, List<String> requiredAttribute) throws JSONException {
        JSONObject jsonBody = new JSONObject(body);

        for(String rA : requiredAttribute){
            if(!(jsonBody.get(rA) instanceof JSONObject)) {
                if (!jsonBody.has(rA) || jsonBody.get(rA).equals(JSONObject.NULL)) {
                    throw new JSONException("Request attribute is required: " + rA);
                }
            }
        }

        return jsonBody;
    }
}
