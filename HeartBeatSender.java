import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.TimerTask;

public class HeartBeatSender extends TimerTask {

    private String contentServerId;
    private String serverIp;
    private int serverPort;

    public HeartBeatSender(String contentServerId, String serverIp, int serverPort) {
        this.contentServerId = contentServerId;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        Socket server;
        try {
            server = new Socket(serverIp, serverPort);
            DataOutputStream dataOutputStream = new DataOutputStream(server.getOutputStream());

            // write the PUT header
            String headerFirstLine = "PUT /putHeartBeat HTTP/1.1";
            String headerSecondLine = "Host: 127.0.0.1:9090";
            String headerThirdLine = "Accept: */*";
            byte[] headerFirstLineByte = headerFirstLine.getBytes(Charset.forName("UTF-8"));
            byte[] headerSecondLineByte = headerSecondLine.getBytes(Charset.forName("UTF-8"));
            byte[] headerThirdLineByte = headerThirdLine.getBytes(Charset.forName("UTF-8"));

            dataOutputStream.writeInt(headerFirstLineByte.length);
            dataOutputStream.write(headerFirstLineByte);
            dataOutputStream.writeInt(headerSecondLineByte.length);
            dataOutputStream.write(headerSecondLineByte);
            dataOutputStream.writeInt(headerThirdLineByte.length);
            dataOutputStream.write(headerThirdLineByte);

            // write content server id
            byte[] contentServerIdByte = contentServerId.getBytes(Charset.forName("UTF-8"));

            dataOutputStream.writeInt(contentServerIdByte.length);
            dataOutputStream.write(contentServerIdByte);

            // write heart beat content
            String heartBeatString = "heart beat";
            byte[] heartBeatByte = heartBeatString.getBytes(Charset.forName("UTF-8"));

            dataOutputStream.writeInt(heartBeatByte.length);
            dataOutputStream.write(heartBeatByte);

            dataOutputStream.close();
            server.close();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}