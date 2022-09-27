import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;

public class ClientHandler implements Runnable {
    private Socket client;
    private DataOutputStream out;
    public final static String FILE_PATH_NAME = "./ATOMFeed.xml";
    private LamportClock lamportClock;

    public ClientHandler(Socket clientSocket, LamportClock lamportClock) throws IOException {
        this.client = clientSocket;
        this.lamportClock = lamportClock;
        out = new DataOutputStream(client.getOutputStream());
    }

    @Override
    public void run() {
        FileInputStream fis = null;

        try {
            File file = new File(FILE_PATH_NAME);
            byte[] XMLByte = new byte[(int) file.length()];

            // turn the file input stream into buffered input stream
            fis = new FileInputStream(file);

            // read the file content into myByteArray
            fis.read(XMLByte);
            System.out.println("[AggregationServer]: Sending " + FILE_PATH_NAME.substring(2) + "(" + XMLByte.length
                    + " bytes) to GETClient");

            // write the GET response
            String headerFirstLine = "HTTP/1.1 201 OK";

            lamportClock.increaseTime();
            String lamportClockInfo = "LamportClock: " + lamportClock.getTime();

            byte[] headerFirstLineByte = headerFirstLine.getBytes(Charset.forName("UTF-8"));
            byte[] lamportClockInfoByte = lamportClockInfo.getBytes(Charset.forName("UTF-8"));

            out.writeInt(headerFirstLineByte.length);
            out.write(headerFirstLineByte);
            out.writeInt(lamportClockInfoByte.length);
            out.write(lamportClockInfoByte);
            out.writeInt(XMLByte.length);
            out.write(XMLByte);

            System.out.println("[AggregationServer]: The aggregated feed has been sent.");
            client.close();
            out.close();
        } catch (IOException e) {
            System.out.println("GETClientHandler failed to work.");
            e.printStackTrace();
        }
    }

}
