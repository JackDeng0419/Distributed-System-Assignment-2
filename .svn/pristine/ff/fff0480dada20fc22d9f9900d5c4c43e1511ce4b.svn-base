import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AggregationServer {

    private static final int PORT = 9090;
    private static ArrayList<ClientHandler> clients = new ArrayList<>();
    private static ArrayList<ContentServerHandler> contentServers = new ArrayList<>();
    private static ExecutorService pool = Executors.newFixedThreadPool(4);

    public static void main(String[] args) throws IOException, InterruptedException {
        ServerSocket listener = new ServerSocket(PORT);

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
                    System.out.println("content connected");
                    ContentServerHandler contentServerHandler = new ContentServerHandler(client);
                    contentServers.add(contentServerHandler);
                    pool.execute(contentServerHandler);
                    break;
                default:
                    break;
            }

        }

    }
}
