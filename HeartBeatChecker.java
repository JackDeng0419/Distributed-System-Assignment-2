import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;

public class HeartBeatChecker extends TimerTask {
    private String contentServerId;
    private BlockingQueue<AggregateMessage> aggregatorQueue;

    public HeartBeatChecker(String contentServerId, BlockingQueue<AggregateMessage> aggregatorQueue) {
        this.contentServerId = contentServerId;
        this.aggregatorQueue = aggregatorQueue;
    }

    @Override
    public void run() {

        // generate an aggregate message of content server disconnection
        AggregateMessage feedExpiredMessage = new AggregateMessage(Constant.CONTENT_SERVER_DISCONNECTION,
                contentServerId);

        // add the message to the queue
        aggregatorQueue.add(feedExpiredMessage);
    }

}
