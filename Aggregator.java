import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Deque;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class Aggregator implements Runnable {

    BlockingQueue<AggregateMessage> aggregatorQueue;
    String aggregatedFilename;
    Deque<Feed> feedQueue;
    private ConcurrentHashMap<String, Timer> contentServersHeartBeatTimersMap;
    private ConcurrentHashMap<String, Timestamp> contentServersMap;

    public Aggregator(BlockingQueue<AggregateMessage> aggregatorQueue, String aggregatedFilename,
            Deque<Feed> feedQueue, ConcurrentHashMap<String, Timer> contentServersHeartBeatTimersMap,
            ConcurrentHashMap<String, Timestamp> contentServersMap) {
        this.aggregatorQueue = aggregatorQueue;
        this.aggregatedFilename = aggregatedFilename;
        this.feedQueue = feedQueue;
        this.contentServersHeartBeatTimersMap = contentServersHeartBeatTimersMap;
        this.contentServersMap = contentServersMap;
    }

    @Override
    public void run() {

        AggregateMessage message;
        File outputFile = new File("ATOMFeed.xml");
        try (FileOutputStream os = new FileOutputStream(outputFile, true)) {
            while (true) {
                try {
                    message = aggregatorQueue.take();

                    if (message.operationType == Constant.CONTENT_SERVER_DISCONNECTION) {
                        // content server disconnected
                        final String contentServerId = message.contentServerId;
                        System.out.println(
                                "[AggregationServer]: Feed from ContentServer:" + contentServerId
                                        + " expired!");

                        feedQueue.removeIf(feed -> {
                            return feed.getContentServerId().equals(contentServerId);
                        });
                        XMLCreator.createXML(feedQueue);
                        if (contentServersHeartBeatTimersMap.get(contentServerId) != null) {
                            contentServersHeartBeatTimersMap.get(contentServerId).cancel();
                        }
                        contentServersHeartBeatTimersMap.remove(contentServerId);
                        contentServersMap.remove(contentServerId);
                        System.out.println("[AggregationServer]: Feed from ContentServer:" + contentServerId
                                + " has been removed!");
                    } else if (message.operationType == Constant.PUT_FEED) {

                        // parse the input XML file
                        Feed feed = message.feed;

                        feedQueue.push(feed);

                        // only keep the latest 20 feeds
                        while (feedQueue.size() > 20) {
                            feedQueue.pollLast();
                        }

                        // construct the XML file based on the feed queue
                        System.out.println("[AggregationServer]: Constructing the aggregation XML file...");
                        XMLCreator.createXML(feedQueue);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
