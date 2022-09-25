import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.UUID;

/**
 * contentServer
 * read the command line to find the server name and port number (in URL format)
 * and will send a GET request for the ATOM feed
 */
public class GETClient {

    private static String SERVER_IP;
    private static int SERVER_PORT;
    private static Deque<Feed> feedQueue = new LinkedList<Feed>();
    private static UUID uuid;

    public static void main(String[] args) throws IOException {

        // generate an unique id for this client
        uuid = UUID.randomUUID();

        // receive user input
        String URL = args[0];

        // get the domain and port
        String[] domainPort = URL.split(":", 2);
        SERVER_IP = domainPort[0];
        SERVER_PORT = Integer.parseInt(domainPort[1]);

        Socket server = new Socket(SERVER_IP, SERVER_PORT);

        // sending the get request
        DataOutputStream dataOutputStream = new DataOutputStream(server.getOutputStream());

        // write the GET header
        String headerFirstLine = "GET /getFeed HTTP/1.1";
        String headerSecondLine = "Host: 127.0.0.1:9090";
        String headerThirdLine = "Accept: application/xml";
        byte[] headerFirstLineByte = headerFirstLine.getBytes(Charset.forName("UTF-8"));
        byte[] headerSecondLineByte = headerSecondLine.getBytes(Charset.forName("UTF-8"));
        byte[] headerThirdLineByte = headerThirdLine.getBytes(Charset.forName("UTF-8"));

        dataOutputStream.writeInt(headerFirstLineByte.length);
        dataOutputStream.write(headerFirstLineByte);
        dataOutputStream.writeInt(headerSecondLineByte.length);
        dataOutputStream.write(headerSecondLineByte);
        dataOutputStream.writeInt(headerThirdLineByte.length);
        dataOutputStream.write(headerThirdLineByte);

        // receiving the response from the aggregation server
        DataInputStream dataInputStream = new DataInputStream(server.getInputStream());
        int responseHeaderFirstLineLength = dataInputStream.readInt();
        byte[] responseHeaderFirstLineByte = new byte[responseHeaderFirstLineLength];
        dataInputStream.readFully(responseHeaderFirstLineByte, 0, responseHeaderFirstLineLength);
        String responseHeaderFirstLine = new String(responseHeaderFirstLineByte);
        System.out.println(responseHeaderFirstLine);

        int responseXMLLength = dataInputStream.readInt();
        byte[] responseXMLByte = new byte[responseXMLLength];
        dataInputStream.readFully(responseXMLByte, 0, responseXMLLength);

        // write the XML into a temp file
        File outputFile = new File("GETClientXML/" + uuid.toString() + ".xml");
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        fileOutputStream.write(responseXMLByte);
        File tempXMFile = new File("GETClientXML/" + uuid.toString() + ".xml");

        // parse the responded XML file
        feedQueue = XMLParser.getFeedQueueFromAggregatedXML(tempXMFile);
        printAggregatedFeed();

        fileOutputStream.close();
        outputFile.delete();
    }

    private static void printAggregatedFeed() {
        for (Feed feed : feedQueue) {
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
}