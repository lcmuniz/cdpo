package cephandler.cdpo.composer.helper;

import cephandler.helper.LocalPersistenceMappingHelper;
import cephandler.utils.InterscityEventType;

import java.io.IOException;


/**
 * A singleton to handle the Interscity interaction.
 *
 */
public class CdpoMessageHelper {
    private static CdpoMessageHelper instance;
    private String REDIS_COMPOSER_HOST = System.getenv("REDIS_COMPOSER_HOST");

    private LocalPersistenceMappingHelper rscUuidToInterEventType;

    private CdpoMessageHelper(){
        this.rscUuidToInterEventType = new LocalPersistenceMappingHelper(REDIS_COMPOSER_HOST);
    }



    /**
     * Take the resource informations from Interscity plattaform and generate the InterscityEventType object.
     *
     * @param resourceUuid
     * @param capability
     * @return
     * @throws IOException
     */
    public synchronized InterscityEventType searchForInterscityEventType(String resourceUuid, String capability) throws IOException {
        String hashKey = resourceUuid+"."+capability;
        InterscityEventType interscityEventType = null;
        String jsonResourceData;

        try {
            interscityEventType = rscUuidToInterEventType.get(hashKey);

        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }

        return interscityEventType;

    }


    public static CdpoMessageHelper getInstance(){
        if(instance == null){
            instance = new CdpoMessageHelper();
        }

        return instance;
    }
}
