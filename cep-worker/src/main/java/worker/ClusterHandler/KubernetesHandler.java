package worker.ClusterHandler;

public class KubernetesHandler implements ClusterHandler {

    public KubernetesHandler(){

    }


    public boolean instantiateNewWorker(){

        return false;
    }

    public void StopAndRemoveWorkerFromCluster(String WorkerId){



    }
}
