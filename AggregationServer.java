import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

public class AggregationServer {

    private static final int PORT = 9090;
    private static final String AGGREGATED_FILE_NAME = "ATOMFeed.xml";
    private static ArrayList<ClientHandler> clients = new ArrayList<>();
    private static ArrayList<ContentServerHandler> contentServers = new ArrayList<>();
    private static ExecutorService pool = Executors.newFixedThreadPool(5);

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocket listener = new ServerSocket(PORT);
        PriorityBlockingQueue<Message> priorityQueue = new PriorityBlockingQueue<Message>(20, new MessageComparator());
        FileHandler fileHandler = new FileHandler(priorityQueue, AGGREGATED_FILE_NAME);
        pool.execute(fileHandler);

        while (true) {
            Socket client = listener.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String request = in.readLine();
            String[] requestInfo = request.split(" ", 3);

            switch (requestInfo[1]) {
                case "/getFeed":
                    System.out.println("client connected");
                    ClientHandler clientThread = new ClientHandler(client);
                    clients.add(clientThread);
                    pool.execute(clientThread);
                    break;
                case "/putContent":
                    System.out.println("content server connected");
                    ContentServerHandler contentServerHandler = new ContentServerHandler(client,
                            priorityQueue);
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
