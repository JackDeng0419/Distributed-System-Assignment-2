import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

import javax.naming.spi.StateFactory;

public class AggregationServer {

    private static final int PORT = 9090;
    private static final String AGGREGATED_FILE_NAME = "ATOMFeed.xml";
    private static ArrayList<ClientHandler> clients = new ArrayList<>();
    private static ArrayList<ContentServerHandler> contentServers = new ArrayList<>();
    private static Deque<Feed> feedQueue = new LinkedList<Feed>();
    private static ExecutorService pool = Executors.newFixedThreadPool(5);

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocket listener = new ServerSocket(PORT);
        PriorityBlockingQueue<Message> priorityQueue = new PriorityBlockingQueue<Message>(20, new MessageComparator());
        FileHandler fileHandler = new FileHandler(priorityQueue, AGGREGATED_FILE_NAME, feedQueue);
        new Thread(fileHandler).start();

        // TODO: Construct the feed Queue from the XML file 

        while (true) {
            Socket client = listener.accept();

            DataInputStream dataInputStream = new DataInputStream(client.getInputStream());

            // reading request type
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

            switch (requestTypeInfo[1]) {
                case "/getFeed":
                    System.out.println("client connected");
                    ClientHandler clientThread = new ClientHandler(client);
                    clients.add(clientThread);
                    pool.execute(clientThread);
                    break;
                case "/putContent":
                    System.out.println("content server connected");
                    ContentServerHandler contentServerHandler = new ContentServerHandler(client,
                            priorityQueue, dataInputStream);
                    contentServers.add(contentServerHandler);
                    pool.execute(contentServerHandler);
                    break;
                default:
                    break;
            }

        }

    }
}

/**
 * Comparator for the priority queue
 */
class MessageComparator implements Comparator<Message> {
    public int compare(Message m1, Message m2) {
        if (m1.operationType < m2.operationType)
            return 1;
        else if (m1.operationType > m2.operationType)
            return -1;
        return 0;
    }
}
