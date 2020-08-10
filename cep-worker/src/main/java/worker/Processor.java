package worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import worker.DatabaseAccess.Lettuce;
import worker.ThreadHandler.ThreadHandler;

public class Processor {

    private static Logger LOG = LoggerFactory.getLogger(Processor.class);

    public static void main(String[] args) {

        String WorkerId = null;
        String mode = null;

        /*if (args.length < 1) {
            System.out.print("Worker Id not provided");
            System.exit(0);
        }

        WorkerId = args[0];

        if (args.length < 2) {
            System.out.print("Runtime mode not provided");
            System.exit(0);
        }

        mode = args[1];
        */

        WorkerId = System.getenv("WORKER_ID");
        mode = System.getenv("MODE");
        String redisHost = System.getenv("REDIS_CEP_HOST");


        LOG.info("Creating Thread Handler");
        final ThreadHandler threadHandler = new ThreadHandler(WorkerId,redisHost);

        System.out.print("Worker Started\n");

        //For single node execution
        if(mode.equalsIgnoreCase("mono")) {
            threadHandler.ContinuousSingleProcessing();

            //For multi node execution
        }else {
            //thread for creating threads that accept new eventTypes
            threadHandler.InitiateAcceptionOfEvents();

            // Thread to relocate events to other workers in case of Underload
            threadHandler.InitiateUnderloadMonitoring();

            //Thread to rellocate events to other workers in case of Overload
            threadHandler.InitiateOverloadMonitoring();

        }
    }
}
