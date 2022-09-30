import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Deque;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/* 
 * Aggregator is a consumer of the aggregator queue and perform operations to the aggregation file 
 */
public class Aggregator implements Runnable {

    BlockingQueue<AggregateMessage> aggregatorQueue;
    String aggregationFilename;
    Deque<Feed> feedQueue;
    private ConcurrentHashMap<String, Timer> heartbeatTimersMap;
    private ConcurrentHashMap<String, Timestamp> contentServersMap;

    /*
     * aggregatorQueue: the queue that stores the request message
     * aggregationFilename: the filename of the aggregation XML file
     * feedQueue: the feed queue storing all the active feeds
     * heartbeatTimersMap: the map of the heart beat timer for each content server
     * contentServersMap: the map of the last interaction time
     */
    public Aggregator(BlockingQueue<AggregateMessage> aggregatorQueue, String aggregationFilename,
            Deque<Feed> feedQueue, ConcurrentHashMap<String, Timer> heartbeatTimersMap,
            ConcurrentHashMap<String, Timestamp> contentServersMap) {
        this.aggregatorQueue = aggregatorQueue;
        this.aggregationFilename = aggregationFilename;
        this.feedQueue = feedQueue;
        this.heartbeatTimersMap = heartbeatTimersMap;
        this.contentServersMap = contentServersMap;
    }

    @Override
    public void run() {

        AggregateMessage message;
        File outputFile = new File(aggregationFilename);

        try (FileOutputStream os = new FileOutputStream(outputFile, true)) {
            while (true) {
                try {

                    message = aggregatorQueue.take();

                    if (message.operationType == Constant.CONTENT_SERVER_DISCONNECTION) {
                        /* content server disconnected */

                        final String contentServerId = message.contentServerId;

                        System.out.println(
                                "[AggregationServer]: Feed from ContentServer:" + contentServerId
                                        + " expired!");

                        // remove the corresponding feeds in the feed queue
                        feedQueue.removeIf(feed -> {
                            return feed.getContentServerId().equals(contentServerId);
                        });

                        // regenerate the Aggregation file based on the feed queue
                        XMLCreator.createXML(feedQueue);

                        // cancel and remove the timer for the disconnected content server
                        if (heartbeatTimersMap.get(contentServerId) != null) {
                            heartbeatTimersMap.get(contentServerId).cancel();
                        }
                        heartbeatTimersMap.remove(contentServerId);

                        // remove the content server from the map
                        contentServersMap.remove(contentServerId);

                        System.out.println("[AggregationServer]: Feed from ContentServer:" + contentServerId
                                + " has been removed!");

                    } else if (message.operationType == Constant.PUT_FEED) {
                        /* content server putting feed */
                        Feed feed = message.feed;

                        // put the new feed into the feed queue
                        feedQueue.push(feed);

                        // only keep the latest 20 feeds
                        while (feedQueue.size() > 20) {
                            feedQueue.pollLast();
                        }

                        // generate the XML file based on the feed queue
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
