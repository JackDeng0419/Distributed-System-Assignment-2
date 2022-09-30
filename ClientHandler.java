import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

/* 
 * This class handle the client get request, sending back the aggregation feed XML
 */
public class ClientHandler implements Runnable {
    public final static String FILE_PATH_NAME = "./ATOMFeed.xml";
    private Socket client;
    private DataOutputStream dataOutputStream;
    private LamportClock lamportClock;

    /*
     * clientSocket: the socket of the get client
     * lamportClock: the lamport clock of the aggregation server
     */
    public ClientHandler(Socket clientSocket, LamportClock lamportClock) throws IOException {
        this.client = clientSocket;
        this.lamportClock = lamportClock;
        dataOutputStream = new DataOutputStream(client.getOutputStream());
    }

    @Override
    public void run() {
        try {

            // get the aggregation file
            File file = new File(FILE_PATH_NAME);

            System.out.println("[AggregationServer]: Sending " + FILE_PATH_NAME.substring(2) + "(" + file.length()
                    + " bytes) to GETClient");

            lamportClock.increaseTime();

            // write the GET response and send the aggregation feed
            HTTPUtils.sendString(dataOutputStream, "HTTP/1.1 201 OK");
            HTTPUtils.sendString(dataOutputStream, "LamportClock: " + lamportClock.getTime());
            HTTPUtils.sendFile(dataOutputStream, file);

            System.out.println("[AggregationServer]: The aggregated feed has been sent.");
            client.close();
            dataOutputStream.close();
        } catch (IOException e) {
            System.out.println("GETClientHandler failed to work.");
            e.printStackTrace();
        }
    }

}
