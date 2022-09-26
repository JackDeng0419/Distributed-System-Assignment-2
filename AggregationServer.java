import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;

public class AggregationServer {

    private static final int PORT = 4567;
    private static final String AGGREGATED_FILE_NAME = "ATOMFeed.xml";
    private static Deque<Feed> feedQueue = new LinkedList<Feed>();
    private static ExecutorService pool = Executors.newFixedThreadPool(5);
    private static ConcurrentHashMap<String, Timestamp> contentServersMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Timer> contentServersHeartBeatTimersMap = new ConcurrentHashMap<>();
    private static Socket requestSocket;
    private static BlockingQueue<Message> aggregatorQueue;

    public static void main(String[] args) throws IOException, InterruptedException {

        recoveryFeedQueue();

        ServerSocket listener = new ServerSocket(PORT);

        // initialize the file handler
        aggregatorQueue = new LinkedBlockingDeque<Message>();
        Aggregator aggregator = new Aggregator(aggregatorQueue, AGGREGATED_FILE_NAME, feedQueue);
        new Thread(aggregator).start();

        while (true) {
            requestSocket = listener.accept();

            DataInputStream dataInputStream = new DataInputStream(requestSocket.getInputStream());

            // reading request type
            String[] requestTypeInfo = parseRequestInfo(dataInputStream);

            if (!requestTypeInfo[0].equals("GET") && !requestTypeInfo[0].equals("PUT")) {
                DataOutputStream out = new DataOutputStream(requestSocket.getOutputStream());
                String responseHeaderFirstLine = "HTTP/1.1 400 invalid request type";
                byte[] responseHeaderFirstLineByte = responseHeaderFirstLine.getBytes(Charset.forName("UTF-8"));
                out.writeInt(responseHeaderFirstLineByte.length);
                out.write(responseHeaderFirstLineByte);
                out.close();
                continue;
            }

            switch (requestTypeInfo[1]) {
                case "/getFeed":
                    processGetFeed();
                    break;
                case "/putContent":
                    processPutContent(dataInputStream);
                    break;
                case "/putHeartBeat":
                    processPutHeartBeat(dataInputStream);
                    break;
                default:
                    break;
            }

        }

    }

    private static void recoveryFeedQueue() {
        File aggregatedXML = new File(AGGREGATED_FILE_NAME);
        feedQueue = XMLParser.getFeedQueueFromAggregatedXML(aggregatedXML);
        System.out.println(feedQueue.size());
        for (Feed feed : feedQueue) {
            contentServersMap.put(feed.getContentServerId(), Timestamp.from(Instant.now()));
            Timer timer = new Timer();
            timer.schedule(new HeartBeatChecker(contentServersMap, feed.getContentServerId(), feedQueue,
                    contentServersHeartBeatTimersMap), 12000L);
            contentServersHeartBeatTimersMap.put(feed.getContentServerId(), timer);
        }
    }

    private static String[] parseRequestInfo(DataInputStream dataInputStream) {
        try {
            int headerFirstLineByteLength = dataInputStream.readInt();
            byte[] headerFirstLineByte = new byte[headerFirstLineByteLength];
            dataInputStream.readFully(headerFirstLineByte, 0, headerFirstLineByteLength);
            String headerFirstLine = new String(headerFirstLineByte);
            String[] requestTypeInfo = headerFirstLine.split(" ", 3);

            // reading second and third header lines
            int headerSecondLineByteLength = dataInputStream.readInt();
            byte[] headerSecondLineByte = new byte[headerSecondLineByteLength];
            dataInputStream.readFully(headerSecondLineByte, 0, headerSecondLineByteLength);

            int headerThirdLineByteLength = dataInputStream.readInt();
            byte[] headerThirdLineByte = new byte[headerThirdLineByteLength];
            dataInputStream.readFully(headerThirdLineByte, 0, headerThirdLineByteLength);
            return requestTypeInfo;
        } catch (IOException e) {
            System.out.println("Server is not working");
            e.printStackTrace();
        }

        return new String[2];
    }

    private static void processGetFeed() {
        System.out.println("client connected");
        ClientHandler clientThread;
        try {
            clientThread = new ClientHandler(requestSocket);
            pool.execute(clientThread);
        } catch (IOException e) {
            System.out.println("Aggregation server failed to process /getFeed.");
            e.printStackTrace();
        }
    }

    private static void processPutContent(DataInputStream dataInputStream) {
        System.out.println("content server connected");
        PutFeedHandler putFeedHandler;
        try {
            putFeedHandler = new PutFeedHandler(requestSocket,
                    aggregatorQueue, dataInputStream, contentServersMap,
                    feedQueue, contentServersHeartBeatTimersMap);
            pool.execute(putFeedHandler);
        } catch (IOException e) {
            System.out.println("Aggregation server failed to process /putContent");
            e.printStackTrace();
        }
    }

    private static void processPutHeartBeat(DataInputStream dataInputStream) {
        System.out.println("content server heart beat");
        int contentServerIdByteLength;
        try {
            contentServerIdByteLength = dataInputStream.readInt();
            byte[] contentServerIdByte = new byte[contentServerIdByteLength];
            dataInputStream.readFully(contentServerIdByte, 0, contentServerIdByteLength);
            String contentServerId = new String(contentServerIdByte);
            contentServersMap.put(contentServerId, Timestamp.from(Instant.now()));
            if (contentServersHeartBeatTimersMap.get(contentServerId) == null) {
                Timer timer = new Timer();
                timer.schedule(new HeartBeatChecker(contentServersMap, contentServerId, feedQueue,
                        contentServersHeartBeatTimersMap), 12000L);
                contentServersHeartBeatTimersMap.put(contentServerId, timer);
            } else {
                contentServersHeartBeatTimersMap.get(contentServerId).cancel();
                Timer timer = new Timer();
                timer.schedule(new HeartBeatChecker(contentServersMap, contentServerId, feedQueue,
                        contentServersHeartBeatTimersMap), 12000L);
                contentServersHeartBeatTimersMap.put(contentServerId, timer);
            }

            DataOutputStream out = new DataOutputStream(requestSocket.getOutputStream());

            String responseHeaderFirstLine = "HTTP/1.1 200 OK";
            byte[] responseHeaderFirstLineByte = responseHeaderFirstLine.getBytes(Charset.forName("UTF-8"));
            out.writeInt(responseHeaderFirstLineByte.length);
            out.write(responseHeaderFirstLineByte);

            String responseHeaderSecondLine = "Heart beat signal received.";
            byte[] responseHeaderSecondLineByte = responseHeaderSecondLine.getBytes(Charset.forName("UTF-8"));
            out.writeInt(responseHeaderSecondLineByte.length);
            out.write(responseHeaderSecondLineByte);

            dataInputStream.close();
            out.close();
            requestSocket.close();
        } catch (IOException e) {
            System.out.println("Aggregation server failed to process /putHeartBeat");
            e.printStackTrace();
        }
    }
}
