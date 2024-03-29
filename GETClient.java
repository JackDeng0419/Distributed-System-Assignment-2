import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class GETClient {

    private static Socket server;
    private static String SERVER_IP;
    private static int SERVER_PORT;
    private static Deque<Feed> feedQueue = new LinkedList<Feed>();
    private static String clientId;
    private static LamportClock lamportClock;
    private static Timer retryTimer;
    private static int receiveRetryCount = 0;

    public static void main(String[] args) throws IOException, InterruptedException {

        // receive user input
        String URL = args[0];
        clientId = args[1];

        // get the domain and port of aggregation server
        String[] domainPort = URL.split(":", 2);
        SERVER_IP = domainPort[0];
        SERVER_PORT = Integer.parseInt(domainPort[1]);

        // create the socket of AG
        // if fail to connect to the aggregation server, retry every 2 seconds.
        for (int i = 0; i < 4; i++) {
            if (i == 3) {
                // after 3 times of retry, exit the program
                System.out.println("[GETClient:" + clientId + "]: "
                        + "Failed to connect to AG, please check whether AG is running.");
                System.exit(-1);
            }
            try {
                server = new Socket(SERVER_IP, SERVER_PORT);
                break;
            } catch (IOException e) {
                System.out.println("[GETClient:" + clientId + "]: " + "Reconnect to AG " + (i + 1));
                Thread.sleep(2000);
            }
        }

        // initialize the lamport clock
        lamportClock = new LamportClock(clientId, Constant.HOST_TYPE_GET_CLIENT);

        // sending the get request to AG
        DataOutputStream dataOutputStream = new DataOutputStream(server.getOutputStream());
        sendGETRequest(dataOutputStream);

        // start the retry timer, if not getting response in 3 seconds, resend the
        // request every 3 seconds until receiving the response
        retryTimer = new Timer();
        retryTimer.schedule((new GETClient()).new ReceiveErrorRetry(), 3000L, 3000L);

        // receiving the response from AG
        byte[] responseXMLByte = receiveServerResponse();

        // present the response
        if (responseXMLByte.length != 0) {
            // parse the responded XML file
            feedQueue = generateFeedQueue(responseXMLByte);
            // print the parsed XML content
            printAggregatedFeed();
        }

        server.close();
    }

    /*
     * This methods output the feed content to the terminal according to the feed
     * queue
     */
    private static void printAggregatedFeed() {
        ArrayList<Feed> feedArrayList = new ArrayList<>(feedQueue);
        feedArrayList.sort(Comparator.comparing(Feed::getContentServerId));
        for (Feed feed : feedArrayList) {
            System.out.println("Title: " + feed.getTitle());
            System.out.println("Subtitle: " + feed.getSubtitle());
            System.out.println("Link: " + feed.getLink());
            System.out.println("Updated: " + feed.getUpdated());
            System.out.println("Author: " + feed.getAuthor());
            System.out.println("Id: " + feed.getId());
            for (FeedEntry feedEntry : feed.getEntries()) {
                System.out.println("Entry:");
                System.out.println("Title: " + feedEntry.getTitle());
                System.out.println("Link: " + feedEntry.getLink());
                System.out.println("Id: " + feedEntry.getId());
                System.out.println("Updated: " + feedEntry.getUpdated());
                System.out.println("Author: " + feedEntry.getAuthor());
                System.out.println("Summary: " + feedEntry.getSummary());
            }
            System.out.println("====================");
        }
    }

    /*
     * This method sends the get request to the aggregation server
     */
    private static void sendGETRequest(DataOutputStream dataOutputStream) {

        lamportClock.increaseTime();

        try {
            HTTPUtils.sendString(dataOutputStream, "GET /getFeed HTTP/1.1");
            HTTPUtils.sendString(dataOutputStream, "Host: " + SERVER_IP + ":" + SERVER_PORT);
            HTTPUtils.sendString(dataOutputStream, "Accept: application/xml");
            HTTPUtils.sendString(dataOutputStream, "LamportClock: " + lamportClock.getTime());
        } catch (IOException e) {
            System.out.println("GETClient failed to send get request.");
            System.out.println("Detail: ");
            e.printStackTrace();
        }
    }

    /*
     * This method receives the response from the aggregation server
     */
    private static byte[] receiveServerResponse() {

        try (DataInputStream dataInputStream = new DataInputStream(server.getInputStream())) {
            // read the response header
            String responseHeaderFirstLine = HTTPUtils.readString(dataInputStream);
            System.out.println(responseHeaderFirstLine);

            // read the lamport clock
            String[] lamportStrings = HTTPUtils.readString(dataInputStream).split(": ");
            lamportClock.update(Integer.parseInt(lamportStrings[1]));

            // read the response aggregation XML
            int responseXMLLength = dataInputStream.readInt();
            byte[] responseXMLByte = new byte[responseXMLLength];
            dataInputStream.readFully(responseXMLByte, 0, responseXMLLength);

            // cancel the retry timer for getting response
            if (null != retryTimer) {
                retryTimer.cancel();
            }
            return responseXMLByte;
        } catch (IOException e) {
            System.out.println("GETClient failed to receive server response.");
            System.out.println("Detail:");
            e.printStackTrace();
            return null;
        }
    }

    /*
     * This method generates a feed queue from the received XML bytes.
     */
    private static Deque<Feed> generateFeedQueue(byte[] responseXMLByte) {
        // write the XML into a temp file
        File outputFile = new File("GETClientXML/" + clientId + ".xml");
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            fileOutputStream.write(responseXMLByte);
            fileOutputStream.close();
        } catch (IOException e) {
            System.out.println("GETClient failed to save the XML file.");
            System.out.println("Detail:");
            e.printStackTrace();
        }

        // generate the feed queue from the temp file
        Deque<Feed> feedQueue = XMLParser.getFeedQueueFromAggregatedXML(outputFile);
        outputFile.delete();
        return feedQueue;
    }

    /**
     * This class is a timer task that resends the request when does not get a
     * response from the aggregation server
     */
    public class ReceiveErrorRetry extends TimerTask {

        @Override
        public void run() {
            GETClient.receiveRetryCount++;
            if (GETClient.receiveRetryCount > 3) {
                // exit the program
                System.out.println(
                        "[GETClient:" + clientId + "]: " + "No response from AG. Please make sure the AG is working.");
                retryTimer.cancel();
                System.exit(0);
            } else {
                System.out.println("[GETClient:" + clientId + "]: " + "Resend get request " + receiveRetryCount);
                try {
                    DataOutputStream dataOutputStream = new DataOutputStream(server.getOutputStream());
                    GETClient.sendGETRequest(dataOutputStream);
                } catch (IOException e) {
                    System.out.println("The AG socket failed to close");
                    e.printStackTrace();
                }
            }

        }

    }
}