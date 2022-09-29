import java.sql.Timestamp;
import java.util.Deque;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class HeartBeatChecker extends TimerTask {
    private String contentServerId;
    private BlockingQueue<AggregateMessage> aggregatorQueue;

    public HeartBeatChecker(ConcurrentHashMap<String, Timestamp> contentServersMap, String contentServerId,
            Deque<Feed> feedQueue, ConcurrentHashMap<String, Timer> contentServersHeartBeatTimersMap,
            BlockingQueue<AggregateMessage> aggregatorQueue) {
        this.contentServerId = contentServerId;
        this.aggregatorQueue = aggregatorQueue;
    }

    @Override
    public void run() {

        AggregateMessage feedExpiredMessage = new AggregateMessage(Constant.CONTENT_SERVER_DISCONNECTION,
                contentServerId);

        aggregatorQueue.add(feedExpiredMessage);
    }

}
