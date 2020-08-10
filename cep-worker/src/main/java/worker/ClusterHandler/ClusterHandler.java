package worker.ClusterHandler;

public interface ClusterHandler {

    boolean instantiateNewWorker();

    void StopAndRemoveWorkerFromCluster(String WorkerId);
}
