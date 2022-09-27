import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Deque;
import java.util.concurrent.BlockingQueue;

public class Aggregator implements Runnable {

    BlockingQueue<Message> priorityQueue;
    String aggregatedFilename;
    Deque<Feed> feedQueue;

    public Aggregator(BlockingQueue<Message> priorityQueue, String aggregatedFilename, Deque<Feed> feedQueue) {
        this.priorityQueue = priorityQueue;
        this.aggregatedFilename = aggregatedFilename;
        this.feedQueue = feedQueue;
    }

    @Override
    public void run() {

        Message message;
        File outputFile = new File("ATOMFeed.xml");
        try (FileOutputStream os = new FileOutputStream(outputFile, true)) {
            while (true) {
                try {
                    message = priorityQueue.take();

                    if (message.operationType == 99) {
                        // content server disconnected
                        System.out.println("content server disconnected.");
                    } else if (message.operationType == 1) {
                        FileOutputStream tempXMLFileOutputStream = new FileOutputStream(
                                "AggregationServerXML/" + message.contentServerId + ".xml");
                        tempXMLFileOutputStream.write(message.payload);
                        tempXMLFileOutputStream.close();
                        File tempXMLFile = new File("AggregationServerXML/" + message.contentServerId + ".xml");

                        // parse the input XML file
                        Feed feed = XMLParser.parseXMLFile(tempXMLFile);
                        feed.setContentServerId(message.contentServerId);

                        feedQueue.push(feed);

                        // only keep the latest 20 feeds
                        while (feedQueue.size() > 20) {
                            feedQueue.pollLast();
                        }

                        // construct the XML file based on the feed queue
                        System.out.println("Writing the file...");
                        // os.write(message.payload);
                        XMLCreator.createXML(feedQueue);

                        tempXMLFile.delete();

                        // construct the XML file based on the txt file
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
