import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Deque;
import java.util.HashMap;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

public class ContentServerHandler implements Runnable {

    private Socket contentServer;
    private OutputStream out;
    private PriorityBlockingQueue<Message> priorityQueue;
    private DataInputStream dataInputStream;
    private ConcurrentHashMap<String, Timestamp> contentServersMap;
    private ConcurrentHashMap<String, Timer> timersMap;
    public final static String FILE_PATH_NAME = "./ATOMFeed.txt";
    private Deque<Feed> feedQueue;
    private ConcurrentHashMap<String, Timer> contentServersHeartBeatTimersMap;

    public ContentServerHandler(Socket contentServerSocket, PriorityBlockingQueue<Message> priorityQueue,
            DataInputStream dataInputStream, ConcurrentHashMap<String, Timestamp> contentServersMap,
            ConcurrentHashMap<String, Timer> timersMap, Deque<Feed> feedQueue,
            ConcurrentHashMap<String, Timer> contentServersHeartBeatTimersMap)
            throws IOException {
        this.contentServer = contentServerSocket;
        this.priorityQueue = priorityQueue;
        this.dataInputStream = dataInputStream;
        this.contentServersMap = contentServersMap;
        this.timersMap = timersMap;
        out = contentServer.getOutputStream();
    }

    @Override
    public void run() {
        String contentServerId;
        byte[] payload;

        try {
            // reading content server id
            int contentServerIdByteLength = dataInputStream.readInt();
            byte[] contentServerIdByte = new byte[contentServerIdByteLength];
            dataInputStream.readFully(contentServerIdByte, 0, contentServerIdByteLength);
            contentServerId = new String(contentServerIdByte);

            // reading payload
            int payloadLength = dataInputStream.readInt();
            payload = new byte[payloadLength];
            dataInputStream.readFully(payload, 0, payloadLength);

            // construct the message object
            System.out.println(payload.toString());
            Message message = new Message(GeneralDefinition.PUT_FEED, contentServerId, payload);

            // TODO: move the feed object creation to here

            // TODO: do the XML format check

            // add message to the priority queue
            priorityQueue.add(message);

            if (contentServersMap.get(contentServerId) == null) {
                contentServersMap.put(contentServerId, Timestamp.from(Instant.now()));
                Timer timer = new Timer();
                timer.schedule(new HeartBeatChecker(contentServersMap, contentServerId, feedQueue,
                        contentServersHeartBeatTimersMap), 12000L);
                timersMap.put(contentServerId, timer);
            } else {
                contentServersMap.put(contentServerId, Timestamp.from(Instant.now()));
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        // output the myByteArray to the content server
        PrintWriter printWriter = new PrintWriter(out);
        printWriter.println("New content is saved.");
        printWriter.flush();

        System.out.println("Done");

        try {
            out.close();
            dataInputStream.close();
            contentServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
