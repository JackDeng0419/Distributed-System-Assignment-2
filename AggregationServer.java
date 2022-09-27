import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.time.Instant;
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

/* 
 * Accept and process the request from GETClient and Content Server
 */
public class AggregationServer {

    private static final int PORT = 4567;
    private static Socket requestSocket;
    private static LamportClock lamportClock;
    private static PriorityBlockingQueue<RequestMessage> lamportClockQueue;

    public static void main(String[] args) throws IOException, InterruptedException {

        System.out.println("[AggregationServer]: Aggregation server start");

        lamportClock = new LamportClock();
        lamportClockQueue = new PriorityBlockingQueue<>(20, Comparator.comparing(RequestMessage::getTime));

        // initialize the general request handler
        GeneralRequestHandler generalRequestHandler = new GeneralRequestHandler(lamportClockQueue, lamportClock);
        new Thread(generalRequestHandler).start();

        ServerSocket listener = new ServerSocket(PORT);

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

            lamportClockQueue.add(
                    new RequestMessage(lamportClock.getTime(), requestTypeInfo[1], requestSocket, dataInputStream));

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

            // parse the lamport clock
            int lamportClockInfoByteLength = dataInputStream.readInt();
            byte[] lamportClockInfoByte = new byte[lamportClockInfoByteLength];
            dataInputStream.readFully(lamportClockInfoByte);
            String lamportClockInfo = new String(lamportClockInfoByte);
            String[] tempStrings = lamportClockInfo.split(": ", 2);
            int newTime = Integer.parseInt(tempStrings[1]);
            lamportClock.update(newTime);

            return requestTypeInfo;
        } catch (IOException e) {
            System.out.println("Server is not working");
            e.printStackTrace();
        }

        return new String[2];
    }

}

class GeneralRequestHandler implements Runnable {

    private PriorityBlockingQueue<RequestMessage> lamportClockQueue;
    private Socket requestSocket;
    private LamportClock lamportClock;
    private DataInputStream dataInputStream;
    private ExecutorService pool = Executors.newFixedThreadPool(5);
    private Deque<Feed> feedQueue = new LinkedList<Feed>();
    private final String AGGREGATED_FILE_NAME = "ATOMFeed.xml";
    private ConcurrentHashMap<String, Timestamp> contentServersMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Timer> contentServersHeartBeatTimersMap = new ConcurrentHashMap<>();
    private BlockingQueue<Message> aggregatorQueue;

    public GeneralRequestHandler(PriorityBlockingQueue<RequestMessage> lamportCloBlockingQueue,
            LamportClock lamportClock) {

        this.lamportClockQueue = lamportCloBlockingQueue;
        this.lamportClock = lamportClock;

        recoveryFeedQueue();

        // initialize the file handler
        aggregatorQueue = new LinkedBlockingDeque<Message>();
        Aggregator aggregator = new Aggregator(aggregatorQueue, AGGREGATED_FILE_NAME, feedQueue);
        new Thread(aggregator).start();
    }

    @Override
    public void run() {

        RequestMessage requestMessage;

        while (true) {
            try {
                requestMessage = lamportClockQueue.take();

                this.requestSocket = requestMessage.getRequestSocket();
                this.dataInputStream = requestMessage.getDataInputStream();

                switch (requestMessage.getRequestRoute()) {
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

            } catch (InterruptedException e) {
                System.out.println("The GeneralRequestHandler thread is interrupted.");
                e.printStackTrace();
            }

        }

    }

    private void processGetFeed() {
        System.out.println("[AggregationServer]: Client connected");
        ClientHandler clientThread;
        try {
            clientThread = new ClientHandler(requestSocket, lamportClock);
            pool.execute(clientThread);
        } catch (IOException e) {
            System.out.println("Aggregation server failed to process /getFeed.");
            e.printStackTrace();
        }
    }

    private void processPutContent(DataInputStream dataInputStream) {
        System.out.println("[AggregationServer]: Content server connected");
        PutFeedHandler putFeedHandler;
        try {
            putFeedHandler = new PutFeedHandler(requestSocket,
                    aggregatorQueue, dataInputStream, contentServersMap,
                    feedQueue, contentServersHeartBeatTimersMap, lamportClock);
            pool.execute(putFeedHandler);
        } catch (IOException e) {
            System.out.println("Aggregation server failed to process /putContent");
            e.printStackTrace();
        }
    }

    private void processPutHeartBeat(DataInputStream dataInputStream) {
        System.out.println("[AggregationServer]: Received content server heart beat");
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

            lamportClock.increaseTime();

            DataOutputStream out = new DataOutputStream(requestSocket.getOutputStream());

            String responseHeaderFirstLine = "HTTP/1.1 200 OK";
            byte[] responseHeaderFirstLineByte = responseHeaderFirstLine.getBytes(Charset.forName("UTF-8"));
            out.writeInt(responseHeaderFirstLineByte.length);
            out.write(responseHeaderFirstLineByte);

            String responseHeaderSecondLine = "Heart beat signal received.";
            byte[] responseHeaderSecondLineByte = responseHeaderSecondLine.getBytes(Charset.forName("UTF-8"));
            out.writeInt(responseHeaderSecondLineByte.length);
            out.write(responseHeaderSecondLineByte);

            String lamportClockInfo = "LamportClock: " + lamportClock.getTime();
            byte[] lamportClockInfoByte = lamportClockInfo.getBytes(Charset.forName("UTF-8"));
            out.writeInt(lamportClockInfoByte.length);
            out.write(lamportClockInfoByte);

            dataInputStream.close();
            out.close();
            requestSocket.close();
        } catch (IOException e) {
            System.out.println("Aggregation server failed to process /putHeartBeat");
            e.printStackTrace();
        }
    }

    private void recoveryFeedQueue() {

        File aggregatedXML = new File(AGGREGATED_FILE_NAME);
        if (aggregatedXML.isFile() && aggregatedXML.length() != 0) {
            feedQueue = XMLParser.getFeedQueueFromAggregatedXML(aggregatedXML);
            for (Feed feed : feedQueue) {
                contentServersMap.put(feed.getContentServerId(), Timestamp.from(Instant.now()));
                Timer timer = new Timer();
                timer.schedule(new HeartBeatChecker(contentServersMap, feed.getContentServerId(), feedQueue,
                        contentServersHeartBeatTimersMap), 12000L);
                contentServersHeartBeatTimersMap.put(feed.getContentServerId(), timer);
            }
        }
    }

}

class RequestMessage {

    private int time;

    private String requestRoute;

    private Socket requestSocket;

    public Socket getRequestSocket() {
        return requestSocket;
    }

    private DataInputStream dataInputStream;

    public DataInputStream getDataInputStream() {
        return dataInputStream;
    }

    public RequestMessage(int time, String requestRoute, Socket requestSocket, DataInputStream dataInputStream) {
        this.time = time;
        this.requestRoute = requestRoute;
        this.requestSocket = requestSocket;
        this.dataInputStream = dataInputStream;
    }

    public int getTime() {
        return time;
    }

    public String getRequestRoute() {
        return requestRoute;
    }
}
