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
 * Start an AG server socket and receive request socket
 */
public class AggregationServer {

    private static final int PORT = 4567;
    private static LamportClock lamportClock;
    private static PriorityBlockingQueue<RequestMessage> lamportClockQueue;

    public static void main(String[] args) throws IOException, InterruptedException {

        System.out.println("[AggregationServer]: Aggregation server start");

        // initiate the lamport clock and the priority queue for lamport clock
        lamportClock = new LamportClock();
        lamportClockQueue = new PriorityBlockingQueue<>(20, Comparator.comparing(RequestMessage::getTime));

        // start a thread for the general request handler, which take the request from
        // the lamport clock queue to process
        GeneralRequestHandler generalRequestHandler = new GeneralRequestHandler(lamportClockQueue, lamportClock);
        new Thread(generalRequestHandler).start();

        // get the socket
        ServerSocket listener = new ServerSocket(PORT);

        // keep listening to requests
        while (true) {
            // receive the request socket
            final Socket requestSocket = listener.accept();

            // get datainput stream from the request socket
            DataInputStream dataInputStream = new DataInputStream(requestSocket.getInputStream());

            // reading request type
            String[] requestTypeInfo = parseRequestInfo(dataInputStream);

            // if the request type is not GET or PUT, return status code 400
            if (!requestTypeInfo[0].equals("GET") && !requestTypeInfo[0].equals("PUT")) {
                DataOutputStream out = new DataOutputStream(requestSocket.getOutputStream());
                String responseHeaderFirstLine = "HTTP/1.1 400 invalid request type";
                byte[] responseHeaderFirstLineByte = responseHeaderFirstLine.getBytes(Charset.forName("UTF-8"));
                out.writeInt(responseHeaderFirstLineByte.length);
                out.write(responseHeaderFirstLineByte);
                out.close();
                continue;
            }

            // add the request message to the lamport clock queue
            lamportClockQueue.add(
                    new RequestMessage(lamportClock.getTime(), requestTypeInfo[1], requestSocket, dataInputStream));

        }

    }

    /*
     * parsing the request information and updating the lamport clock
     * 
     * return: a string array, whose first element is the request type and the
     * second element is the request route
     */
    private static String[] parseRequestInfo(DataInputStream dataInputStream) {
        try {
            // read the first header line
            String headerFirstLine = HTTPUtils.readString(dataInputStream);

            // get the string array for request type and request route
            String[] requestTypeInfo = headerFirstLine.split(" ", 3);

            // reading second and third header lines
            HTTPUtils.readString(dataInputStream);
            HTTPUtils.readString(dataInputStream);

            // parse the lamport clock and update
            String lamportClockInfo = HTTPUtils.readString(dataInputStream);
            String[] tempStrings = lamportClockInfo.split(": ", 2);
            int newTime = Integer.parseInt(tempStrings[1]);
            lamportClock.update(newTime);

            return requestTypeInfo;
        } catch (IOException e) {
            System.out.println("Aggregation Server failed to parse request information.");
            e.printStackTrace();
        }

        return new String[2];
    }

}

/*
 * a consumer for the lamport clock queue
 */
class GeneralRequestHandler implements Runnable {

    private final String AGGREGATED_FILE_NAME = "ATOMFeed.xml";
    private PriorityBlockingQueue<RequestMessage> lamportClockQueue;
    private Socket requestSocket;
    private LamportClock lamportClock;
    private DataInputStream dataInputStream;

    // thread pool for handling requests
    private ExecutorService pool = Executors.newFixedThreadPool(5);

    // a queue that stores all the feeds in the aggregation XML
    private Deque<Feed> feedQueue = new LinkedList<Feed>();

    // a map that stores the heart beat timer for each content server
    private ConcurrentHashMap<String, Timestamp> contentServersMap = new ConcurrentHashMap<>();

    // a map that stores the time of last interaction for each content server
    private ConcurrentHashMap<String, Timer> contentServersHeartBeatTimersMap = new ConcurrentHashMap<>();

    // a queue for aggregation XML operations
    private BlockingQueue<AggregateMessage> aggregatorQueue;

    public GeneralRequestHandler(PriorityBlockingQueue<RequestMessage> lamportCloBlockingQueue,
            LamportClock lamportClock) {

        this.lamportClockQueue = lamportCloBlockingQueue;
        this.lamportClock = lamportClock;

        // recovery the feed queue from ATOMFeed.xml
        recoveryFeedQueue();

        // start a new thread for the Aggregator
        aggregatorQueue = new LinkedBlockingDeque<AggregateMessage>();
        Aggregator aggregator = new Aggregator(aggregatorQueue, AGGREGATED_FILE_NAME, feedQueue,
                contentServersHeartBeatTimersMap, contentServersMap);
        new Thread(aggregator).start();
    }

    /*
     * recover the feed queue from the XML file and then recover the content server
     * map and the heart beat timer map
     */
    private void recoveryFeedQueue() {

        File aggregatedXML = new File(AGGREGATED_FILE_NAME);
        if (aggregatedXML.isFile() && aggregatedXML.length() != 0) {
            feedQueue = XMLParser.getFeedQueueFromAggregatedXML(aggregatedXML);

            // also recovery the content server map and the heart beat timer map
            for (Feed feed : feedQueue) {
                contentServersMap.put(feed.getContentServerId(), Timestamp.from(Instant.now()));

                Timer timer = new Timer();

                // set a timer to check the content server last interaction 12 seconds later
                timer.schedule(new HeartBeatChecker(feed.getContentServerId(), aggregatorQueue), 12000L);
                contentServersHeartBeatTimersMap.put(feed.getContentServerId(), timer);
            }
        }
    }

    /*
     * handler for the GET /getFeed
     * pass the request to the ClientHandler
     */
    private void processGetFeed() {
        System.out.println("[AggregationServer]: Client connected");
        ClientHandler clientThread;
        try {
            // start a new thread to run the ClientHandler
            clientThread = new ClientHandler(requestSocket, lamportClock);
            pool.execute(clientThread);
        } catch (IOException e) {
            System.out.println("Aggregation server failed to process /getFeed.");
            e.printStackTrace();
        }
    }

    /*
     * handler for the PUT /putContent
     * pass the request to the PutFeedHandler
     */
    private void processPutContent(DataInputStream dataInputStream) {
        System.out.println("[AggregationServer]: Content server connected");
        PutFeedHandler putFeedHandler;
        try {
            // start a new thread to run the PutFeedHandler
            putFeedHandler = new PutFeedHandler(requestSocket,
                    aggregatorQueue, dataInputStream, contentServersMap, contentServersHeartBeatTimersMap,
                    lamportClock);
            pool.execute(putFeedHandler);
        } catch (IOException e) {
            System.out.println("Aggregation server failed to process /putContent");
            e.printStackTrace();
        }
    }

    /*
     * handler for the CS heart beat
     */
    private void processPutHeartBeat(DataInputStream dataInputStream) {
        System.out.println("[AggregationServer]: Received content server heart beat");
        int contentServerIdByteLength;
        try {
            // get the content server id
            contentServerIdByteLength = dataInputStream.readInt();
            byte[] contentServerIdByte = new byte[contentServerIdByteLength];
            dataInputStream.readFully(contentServerIdByte, 0, contentServerIdByteLength);
            String contentServerId = new String(contentServerIdByte);

            // put the content server id and a timestamp to the map
            contentServersMap.put(contentServerId, Timestamp.from(Instant.now()));

            // cancel the old timer and set a new one
            if (contentServersHeartBeatTimersMap.get(contentServerId) != null) {
                contentServersHeartBeatTimersMap.get(contentServerId).cancel();
            }
            Timer timer = new Timer();
            timer.schedule(new HeartBeatChecker(contentServerId, aggregatorQueue), 12000L);
            contentServersHeartBeatTimersMap.put(contentServerId, timer);

            // increase the lamport clock
            lamportClock.increaseTime();

            /*
             * send the response
             */
            DataOutputStream out = new DataOutputStream(requestSocket.getOutputStream());

            HTTPUtils.sendString(out, "HTTP/1.1 200 OK");
            HTTPUtils.sendString(out, "Heart beat signal received.");
            HTTPUtils.sendString(out, "LamportClock: " + lamportClock.getTime());

            dataInputStream.close();
            out.close();
            requestSocket.close();
        } catch (IOException e) {
            System.out.println("Aggregation server failed to process /putHeartBeat");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        RequestMessage requestMessage;

        while (true) {
            try {
                // take the request from the queue and process it
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

}

/*
 * storing the information of a request
 */
class RequestMessage {

    private int time; // the time of lamport clock of the requester
    private String requestRoute;
    private Socket requestSocket;
    private DataInputStream dataInputStream;

    public RequestMessage(int time, String requestRoute, Socket requestSocket, DataInputStream dataInputStream) {
        this.time = time;
        this.requestRoute = requestRoute;
        this.requestSocket = requestSocket;
        this.dataInputStream = dataInputStream;
    }

    public Socket getRequestSocket() {
        return requestSocket;
    }

    public DataInputStream getDataInputStream() {
        return dataInputStream;
    }

    public int getTime() {
        return time;
    }

    public String getRequestRoute() {
        return requestRoute;
    }
}
