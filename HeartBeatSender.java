import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
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

            // write the PUT header
            String headerFirstLine = "PUT /putHeartBeat HTTP/1.1";
            String headerSecondLine = "Host: " + SERVER_IP + ":" + SERVER_PORT;
            String headerThirdLine = "Accept: */*";
            String lamportClockInfo = "LamportClock: " + lamportClock.getTime();

            byte[] headerFirstLineByte = headerFirstLine.getBytes(Charset.forName("UTF-8"));
            byte[] headerSecondLineByte = headerSecondLine.getBytes(Charset.forName("UTF-8"));
            byte[] headerThirdLineByte = headerThirdLine.getBytes(Charset.forName("UTF-8"));
            byte[] lamportClockInfoByte = lamportClockInfo.getBytes(Charset.forName("UTF-8"));

            dataOutputStream.writeInt(headerFirstLineByte.length);
            dataOutputStream.write(headerFirstLineByte);
            dataOutputStream.writeInt(headerSecondLineByte.length);
            dataOutputStream.write(headerSecondLineByte);
            dataOutputStream.writeInt(headerThirdLineByte.length);
            dataOutputStream.write(headerThirdLineByte);
            dataOutputStream.writeInt(lamportClockInfoByte.length);
            dataOutputStream.write(lamportClockInfoByte);

            // write content server id
            byte[] contentServerIdByte = contentServerId.getBytes(Charset.forName("UTF-8"));

            dataOutputStream.writeInt(contentServerIdByte.length);
            dataOutputStream.write(contentServerIdByte);

            // write heart beat content
            String heartBeatString = "heart beat";
            byte[] heartBeatByte = heartBeatString.getBytes(Charset.forName("UTF-8"));

            dataOutputStream.writeInt(heartBeatByte.length);
            dataOutputStream.write(heartBeatByte);

            // receiving the response from the aggregation server
            DataInputStream dataInputStream = new DataInputStream(server.getInputStream());
            int responseFirstLineLength = dataInputStream.readInt();
            byte[] responseFirstLineByte = new byte[responseFirstLineLength];
            dataInputStream.readFully(responseFirstLineByte, 0, responseFirstLineLength);
            String responseFirstLine = new String(responseFirstLineByte);
            System.out.println(responseFirstLine + " AG received heart beat.");

            int responseSecondLineLength = dataInputStream.readInt();
            byte[] responseSecondLineByte = new byte[responseSecondLineLength];
            dataInputStream.readFully(responseSecondLineByte, 0, responseSecondLineLength);
            String responseSecondLine = new String(responseSecondLineByte);
            System.out.println(responseSecondLine + " AG received heart beat.");

            // receive and update lamport clock
            int responseLamportClockLength = dataInputStream.readInt();
            byte[] responseLamportClockByte = new byte[responseLamportClockLength];
            dataInputStream.readFully(responseLamportClockByte, 0, responseLamportClockLength);
            String responseLamportClock = new String(responseLamportClockByte);
            String[] tempStrings = responseLamportClock.split(": ");
            int newTime = Integer.parseInt(tempStrings[1]);
            lamportClock.update(newTime);

            // dataOutputStream.close();
            // server.close();
        } catch (UnknownHostException e) {
            // e.printStackTrace();
            System.out.println("E1");
        } catch (IOException e) {
            // e.printStackTrace();
            System.out.println("Aggregation Server connection lost...");
        }

    }

}
