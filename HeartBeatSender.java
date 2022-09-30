import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.TimerTask;

public class HeartBeatSender extends TimerTask {

    private String contentServerId;
    private String SERVER_IP;
    private int SERVER_PORT;
    private LamportClock lamportClock;

    public HeartBeatSender(String contentServerId, String SERVER_IP, int SERVER_PORT, LamportClock lamportClock) {
        this.contentServerId = contentServerId;
        this.SERVER_IP = SERVER_IP;
        this.SERVER_PORT = SERVER_PORT;
        this.lamportClock = lamportClock;
    }

    @Override
    public void run() {
        Socket server;
        try {
            server = new Socket(SERVER_IP, SERVER_PORT);
            DataOutputStream dataOutputStream = new DataOutputStream(server.getOutputStream());

            lamportClock.increaseTime();

            /*
             * sending the heart beat signal
             */
            // write the PUT header
            HTTPUtils.sendString(dataOutputStream, "PUT /putHeartBeat HTTP/1.1");
            HTTPUtils.sendString(dataOutputStream, "Host: " + SERVER_IP + ":" + SERVER_PORT);
            HTTPUtils.sendString(dataOutputStream, "Accept: */*");
            HTTPUtils.sendString(dataOutputStream, "LamportClock: " + lamportClock.getTime());

            // write content server id
            HTTPUtils.sendString(dataOutputStream, contentServerId);

            // write heart beat content
            HTTPUtils.sendString(dataOutputStream, "heart beat");

            /*
             * receiving the response from the aggregation server
             */
            DataInputStream dataInputStream = new DataInputStream(server.getInputStream());

            // read the first line and the second line
            String responseFirstLine = HTTPUtils.readString(dataInputStream);
            System.out.println(
                    "[ContentServer:" + contentServerId + "]: " + responseFirstLine + " AG received heart beat");

            HTTPUtils.readString(dataInputStream);

            // read and update lamport clock
            String responseLamportClock = HTTPUtils.readString(dataInputStream);
            String[] tempStrings = responseLamportClock.split(": ");
            int newTime = Integer.parseInt(tempStrings[1]);
            lamportClock.update(newTime);

        } catch (IOException e2) {
            System.out.println("[ContentServer:" + contentServerId + "]: Aggregation Server connection lost...");
        }
    }

}
