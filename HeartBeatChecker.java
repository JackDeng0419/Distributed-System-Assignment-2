import java.sql.Timestamp;
import java.time.Instant;
import java.util.Deque;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class HeartBeatChecker extends TimerTask {
    private ConcurrentHashMap<String, Timestamp> contentServersMap;
    private String contentServerId;
    private Deque<Feed> feedQueue;
    private ConcurrentHashMap<String, Timer> contentServersHeartBeatTimersMap;

    public HeartBeatChecker(ConcurrentHashMap<String, Timestamp> contentServersMap, String contentServerId,
            Deque<Feed> feedQueue, ConcurrentHashMap<String, Timer> contentServersHeartBeatTimersMap) {
        this.contentServersMap = contentServersMap;
        this.contentServerId = contentServerId;
        this.feedQueue = feedQueue;
        this.contentServersHeartBeatTimersMap = contentServersHeartBeatTimersMap;
    }

    @Override
    public void run() {
        System.out.println("Feed expired!");
        feedQueue.removeIf(feed -> {
            return feed.getContentServerId().equals(contentServerId);
        });
        XMLCreator.createXML(feedQueue);
        contentServersHeartBeatTimersMap.get(contentServerId).cancel();
        contentServersHeartBeatTimersMap.remove(contentServerId);
        contentServersMap.remove(contentServerId);
        System.out.println("Feed from " + contentServerId + " has been removed!");
    }

}
