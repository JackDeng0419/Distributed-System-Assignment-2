import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class PutFeedHandler implements Runnable {

    private Socket contentServer;
    private DataOutputStream dataOutputStream;
    private BlockingQueue<AggregateMessage> aggregatorQueue;
    private DataInputStream dataInputStream;
    private ConcurrentHashMap<String, Timestamp> contentServersMap;
    private ConcurrentHashMap<String, Timer> heartbeatTimersMap;
    private String contentServerId;
    private LamportClock lamportClock;
    private byte[] payload;
    private boolean isATOMXMLFormat = true;
    private boolean isNoContent = false;

    public PutFeedHandler(Socket contentServerSocket, BlockingQueue<AggregateMessage> aggregatorQueue,
            DataInputStream dataInputStream, ConcurrentHashMap<String, Timestamp> contentServersMap,
            ConcurrentHashMap<String, Timer> heartbeatTimersMap, LamportClock lamportClock)
            throws IOException {
        this.contentServer = contentServerSocket;
        this.aggregatorQueue = aggregatorQueue;
        this.dataInputStream = dataInputStream;
        this.contentServersMap = contentServersMap;
        this.heartbeatTimersMap = heartbeatTimersMap;
        this.lamportClock = lamportClock;
        dataOutputStream = new DataOutputStream(contentServer.getOutputStream());
    }

    @Override
    public void run() {

        // get the content server id and the payload (upload content)
        readContentServerIdAndPayload();

        // if no content is sent, return status code 204
        if (isNoContent) {
            System.out.println("[AggregationServer]: 204 - no content in the request");
            try {
                HTTPUtils.sendString(dataOutputStream, "HTTP/1.1 204 no content");
            } catch (Exception e) {
                System.out.println("Aggregation failed to send 204 response.");
                e.printStackTrace();
            }
            return;
        }

        // generate a feed object based on the payload
        Feed feed = generateFeedFromPayload();

        // if the feed is not ATOM XML format, return status code 500
        if (feed == null || !isATOMXMLFormat) {
            System.out.println("[AggregationServer]: 500 - not ATOM feed");
            try {
                HTTPUtils.sendString(dataOutputStream, "HTTP/1.1 500 not ATOM feed");
            } catch (IOException e) {
                System.out.println("Aggregation failed to send 500 response.");
                e.printStackTrace();
            }
        } else {

            // construct the message object for Aggregator
            AggregateMessage message = new AggregateMessage(Constant.PUT_FEED, contentServerId, feed);

            // add message to the priority queue, which will then be processed by the
            // aggregator
            aggregatorQueue.add(message);

            // send the PUT response to content server
            sendPutFeedResponse();
        }

        try {
            dataOutputStream.close();
            dataInputStream.close();
            contentServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * this methods read the content server id and the payload from the request
     */
    private void readContentServerIdAndPayload() {
        try {
            // reading content server id
            contentServerId = HTTPUtils.readString(dataInputStream);

            // reading payload
            int payloadLength = dataInputStream.readInt();

            // if no content, return status code 204 and set isNoContent to true
            if (payloadLength == 0) {
                isNoContent = true;
                return;
            }

            payload = new byte[payloadLength];
            dataInputStream.readFully(payload, 0, payloadLength);
        } catch (IOException e) {
            System.out.println("PutFeedHandler failed to read content server id and payload.");
            e.printStackTrace();
        }
    }

    /*
     * return: the feed object generated from the payload
     * 
     * this method parses the payload and generate a feed object
     */
    private Feed generateFeedFromPayload() {
        try {
            // generate a temporary xml file of the sent content from the payload
            File tempXMLFile = new File("AggregationServerXML/" + contentServerId + ".xml");
            FileOutputStream tempXMLFileOutputStream = new FileOutputStream(tempXMLFile);
            tempXMLFileOutputStream.write(payload);

            // parse the temporary file to get a feed object
            Feed feed = XMLParser.parseXMLFile(tempXMLFile);
            feed.setContentServerId(contentServerId);

            // check whether the feed is ATOM XML format
            isATOMXMLFormat = feed.isATOMXMLFormat();

            tempXMLFileOutputStream.close();
            tempXMLFile.delete();
            return feed;
        } catch (IOException e) {
            System.out.println("PutFeedHandler failed to generate feed from payload.");
            e.printStackTrace();
        }
        return null;
    }

    /*
     * This method sends the response to the content server and set the timer for
     * it
     */
    private void sendPutFeedResponse() {
        lamportClock.increaseTime();

        try {
            if (contentServersMap.get(contentServerId) == null) {
                contentServersMap.put(contentServerId, Timestamp.from(Instant.now()));

                // set a timer for checking heart beat signal
                Timer timer = new Timer();
                timer.schedule(new HeartBeatChecker(contentServerId, aggregatorQueue), 12000L);
                heartbeatTimersMap.put(contentServerId, timer);

                HTTPUtils.sendString(dataOutputStream, "HTTP/1.1 201 OK");

            } else {
                contentServersMap.put(contentServerId, Timestamp.from(Instant.now()));

                // cancel the old timer
                if (heartbeatTimersMap.get(contentServerId) != null) {
                    heartbeatTimersMap.get(contentServerId).cancel();
                }

                // set a new timer for checking heart beat signal
                Timer timer = new Timer();
                timer.schedule(new HeartBeatChecker(contentServerId, aggregatorQueue), 12000L);
                heartbeatTimersMap.put(contentServerId, timer);

                HTTPUtils.sendString(dataOutputStream, "HTTP/1.1 200 OK");
            }

            HTTPUtils.sendString(dataOutputStream, "The feed has been received.");
            HTTPUtils.sendString(dataOutputStream, "LamportClock: " + lamportClock.getTime());

        } catch (Exception e) {
            System.out.println("PutFeedHandler failed to send put response.");
            e.printStackTrace();
        }

    }
}
