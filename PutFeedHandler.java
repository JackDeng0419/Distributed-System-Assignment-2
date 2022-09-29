import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Deque;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class PutFeedHandler implements Runnable {

    private Socket contentServer;
    private DataOutputStream out;
    private BlockingQueue<AggregateMessage> aggregatorQueue;
    private DataInputStream dataInputStream;
    private ConcurrentHashMap<String, Timestamp> contentServersMap;
    private Deque<Feed> feedQueue;
    private ConcurrentHashMap<String, Timer> contentServersHeartBeatTimersMap;
    private String contentServerId;
    private LamportClock lamportClock;
    private byte[] payload;

    public PutFeedHandler(Socket contentServerSocket, BlockingQueue<AggregateMessage> aggregatorQueue,
            DataInputStream dataInputStream, ConcurrentHashMap<String, Timestamp> contentServersMap,
            Deque<Feed> feedQueue,
            ConcurrentHashMap<String, Timer> contentServersHeartBeatTimersMap, LamportClock lamportClock)
            throws IOException {
        this.contentServer = contentServerSocket;
        this.aggregatorQueue = aggregatorQueue;
        this.dataInputStream = dataInputStream;
        this.contentServersMap = contentServersMap;
        this.contentServersHeartBeatTimersMap = contentServersHeartBeatTimersMap;
        this.lamportClock = lamportClock;
        out = new DataOutputStream(contentServer.getOutputStream());
    }

    @Override
    public void run() {

        // get the content server id and the payload (upload content)
        readContentServerIdAndPayload();

        // generate a feed object based on the payload
        Feed feed = generateFeedFromPayload();

        // construct the message object for Aggregator
        AggregateMessage message = new AggregateMessage(Constant.PUT_FEED, contentServerId, feed);

        // add message to the priority queue, and then the Aggregator will process the
        // message
        aggregatorQueue.add(message);

        // send the PUT response to content server
        sendPutResponse();

        try {
            out.close();
            dataInputStream.close();
            contentServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readContentServerIdAndPayload() {
        // reading content server id
        int contentServerIdByteLength;
        try {
            contentServerIdByteLength = dataInputStream.readInt();
            byte[] contentServerIdByte = new byte[contentServerIdByteLength];
            dataInputStream.readFully(contentServerIdByte, 0, contentServerIdByteLength);
            contentServerId = new String(contentServerIdByte);

            // reading payload
            int payloadLength = dataInputStream.readInt();
            if (payloadLength == 0) {
                String headerFirstLine = "HTTP/1.1 204 no content";
                byte[] headerFirstLineByte = headerFirstLine.getBytes(Charset.forName("UTF-8"));
                out.writeInt(headerFirstLineByte.length);
                out.write(headerFirstLineByte);

                return;
            }
            payload = new byte[payloadLength];
            dataInputStream.readFully(payload, 0, payloadLength);
        } catch (IOException e) {
            System.out.println("PutFeedHandler failed to read content server id and payload.");
            e.printStackTrace();
        }
    }

    private Feed generateFeedFromPayload() {
        try {
            File tempXMLFile = new File("AggregationServerXML/" + contentServerId + ".xml");
            FileOutputStream tempXMLFileOutputStream = new FileOutputStream(tempXMLFile);
            tempXMLFileOutputStream.write(payload);
            Feed feed = XMLParser.parseXMLFile(tempXMLFile); // TODO: parseXMLFile should check the format of XML
            feed.setContentServerId(contentServerId);

            tempXMLFileOutputStream.close();
            tempXMLFile.delete();

            return feed;
        } catch (IOException e) {
            System.out.println("PutFeedHandler failed to generate feed from payload.");
            e.printStackTrace();
        }
        return null;
    }

    private void sendPutResponse() {
        lamportClock.increaseTime();

        String headerFirstLine;
        String headerSecondLine = "The feed has been received.";
        String lamportClockInfo = "LamportClock: " + lamportClock.getTime();

        try {
            if (contentServersMap.get(contentServerId) == null) {
                contentServersMap.put(contentServerId, Timestamp.from(Instant.now()));

                // set a timer for checking heart beat signal
                Timer timer = new Timer();
                timer.schedule(new HeartBeatChecker(contentServersMap, contentServerId, feedQueue,
                        contentServersHeartBeatTimersMap, aggregatorQueue), 12000L);
                contentServersHeartBeatTimersMap.put(contentServerId, timer);

                headerFirstLine = "HTTP/1.1 201 OK";
            } else {
                if (contentServersHeartBeatTimersMap.get(contentServerId) != null) {
                    contentServersHeartBeatTimersMap.get(contentServerId).cancel();
                }
                Timer timer = new Timer();
                timer.schedule(new HeartBeatChecker(contentServersMap, contentServerId, feedQueue,
                        contentServersHeartBeatTimersMap, aggregatorQueue), 12000L);
                contentServersHeartBeatTimersMap.put(contentServerId, timer);
                contentServersMap.put(contentServerId, Timestamp.from(Instant.now()));
                headerFirstLine = "HTTP/1.1 200 OK";
            }
            byte[] headerFirstLineByte = headerFirstLine.getBytes(Charset.forName("UTF-8"));
            out.writeInt(headerFirstLineByte.length);
            out.write(headerFirstLineByte);
            byte[] headerSecondLineByte = headerSecondLine.getBytes(Charset.forName("UTF-8"));
            out.writeInt(headerSecondLineByte.length);
            out.write(headerSecondLineByte);

            byte[] lamportClockInfoByte = lamportClockInfo.getBytes(Charset.forName("UTF-8"));
            out.writeInt(lamportClockInfoByte.length);
            out.write(lamportClockInfoByte);

        } catch (Exception e) {
            System.out.println("PutFeedHandler failed to send put response.");
            e.printStackTrace();
        }

    }
}
