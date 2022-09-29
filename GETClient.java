import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
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

    /*
     * args[0]: URL of AG (127.0.0.1:4567)
     * args[1]: client id
     */
    public static void main(String[] args) throws IOException, InterruptedException {

        // receive user input
        String URL = args[0];
        clientId = args[1];

        // get the domain and port of AG
        String[] domainPort = URL.split(":", 2);
        SERVER_IP = domainPort[0];
        SERVER_PORT = Integer.parseInt(domainPort[1]);

        // create the socket of AG
        for (int i = 0; i < 4; i++) {
            if (i == 3) {
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

        // start the retry timer
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

    private static void sendGETRequest(DataOutputStream dataOutputStream) {

        lamportClock.increaseTime();

        String headerFirstLine = "GET /getFeed HTTP/1.1";
        String headerSecondLine = "Host: " + SERVER_IP + ":" + SERVER_PORT;
        String headerThirdLine = "Accept: application/xml";
        String lamportClockInfo = "LamportClock: " + lamportClock.getTime();
        byte[] headerFirstLineByte = headerFirstLine.getBytes(Charset.forName("UTF-8"));
        byte[] headerSecondLineByte = headerSecondLine.getBytes(Charset.forName("UTF-8"));
        byte[] headerThirdLineByte = headerThirdLine.getBytes(Charset.forName("UTF-8"));
        byte[] lamportClockInfoByte = lamportClockInfo.getBytes(Charset.forName("UTF-8"));

        try {
            dataOutputStream.writeInt(headerFirstLineByte.length);
            dataOutputStream.write(headerFirstLineByte);
            dataOutputStream.writeInt(headerSecondLineByte.length);
            dataOutputStream.write(headerSecondLineByte);
            dataOutputStream.writeInt(headerThirdLineByte.length);
            dataOutputStream.write(headerThirdLineByte);
            dataOutputStream.writeInt(lamportClockInfoByte.length);
            dataOutputStream.write(lamportClockInfoByte);
        } catch (IOException e) {
            System.out.println("GETClient failed to send get request.");
            System.out.println("Detail: ");
            e.printStackTrace();
        }
    }

    private static byte[] receiveServerResponse() {

        try (DataInputStream dataInputStream = new DataInputStream(server.getInputStream())) {
            int responseHeaderFirstLineLength = dataInputStream.readInt();
            byte[] responseHeaderFirstLineByte = new byte[responseHeaderFirstLineLength];
            dataInputStream.readFully(responseHeaderFirstLineByte, 0, responseHeaderFirstLineLength);
            String responseHeaderFirstLine = new String(responseHeaderFirstLineByte);

            System.out.println(responseHeaderFirstLine);

            int responseLamportClockLength = dataInputStream.readInt();
            byte[] responseLamportClockByte = new byte[responseLamportClockLength];
            dataInputStream.readFully(responseLamportClockByte, 0, responseLamportClockLength);
            String responseLamportClock = new String(responseLamportClockByte);
            String[] tempStrings = responseLamportClock.split(": ");
            int newTime = Integer.parseInt(tempStrings[1]);
            lamportClock.update(newTime);

            int responseXMLLength = dataInputStream.readInt();
            byte[] responseXMLByte = new byte[responseXMLLength];
            dataInputStream.readFully(responseXMLByte, 0, responseXMLLength);

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
        Deque<Feed> feedQueue = XMLParser.getFeedQueueFromAggregatedXML(outputFile);
        outputFile.delete();
        return feedQueue;
    }

    /**
     * InnerGETClient
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