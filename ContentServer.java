import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Timer;

/**
 * contentServer
 */
public class ContentServer {
    private static String SERVER_IP;
    private static int SERVER_PORT;
    private static String contentServerId;
    private static Socket server;
    private static String inputFilename;
    private static LamportClock lamportClock;

    public static void main(String[] args) throws InterruptedException, IOException {

        String URL = args[0];
        inputFilename = args[1];
        contentServerId = args[2];

        System.out.println("[ContentServer:" + contentServerId + "]: Content server is started");

        // initialize the lamport clock
        lamportClock = new LamportClock(contentServerId, Constant.HOST_TYPE_CONTENT_SERVER);

        // get the domain and port
        String[] domainPort = URL.split(":", 2);
        SERVER_IP = domainPort[0];
        SERVER_PORT = Integer.parseInt(domainPort[1]);

        // create the socket of AG
        for (int i = 0; i < 4; i++) {
            if (i == 3) {
                System.out.println("[ContentServer:" + contentServerId + "]: "
                        + "Failed to connect to AG, please check whether AG is running.");
                System.exit(-1);
            }
            try {
                server = new Socket(SERVER_IP, SERVER_PORT);
                break;
            } catch (IOException e) {
                System.out.println("[ContentServer:" + contentServerId + "]: " + "Reconnect to AG " + (i + 1));
                Thread.sleep(2000);
            }
        }
        DataOutputStream dataOutputStream = new DataOutputStream(server.getOutputStream());
        DataInputStream dataInputStream = new DataInputStream(server.getInputStream());

        try {

            // send the HTTP PUT request to AG
            sendPUTFeedRequest(dataOutputStream);

            // receiving the response from the aggregation server
            receiveServerResponse(dataInputStream);

            // start a timer to send heart beat signal
            Timer timer = new Timer();
            timer.schedule(new HeartBeatSender(contentServerId, SERVER_IP, SERVER_PORT, lamportClock), 1000L, 4000L);

            while (true) {
            }
        } catch (Exception e) {
            System.out.println("error: " + e);
        }
    }

    private static void sendPUTFeedRequest(DataOutputStream dataOutputStream) {
        System.out.println("[ContentServer:" + contentServerId + "]: Send put request to AG");

        lamportClock.increaseTime();

        // prepare the header
        String headerFirstLine = "PUT /putContent HTTP/1.1";
        String headerSecondLine = "Host: " + SERVER_IP + ":" + SERVER_PORT;
        String headerThirdLine = "Accept: */*";
        String lamportClockInfo = "LamportClock: " + lamportClock.getTime();

        byte[] headerFirstLineByte = headerFirstLine.getBytes(Charset.forName("UTF-8"));
        byte[] headerSecondLineByte = headerSecondLine.getBytes(Charset.forName("UTF-8"));
        byte[] headerThirdLineByte = headerThirdLine.getBytes(Charset.forName("UTF-8"));
        byte[] lamportClockInfoByte = lamportClockInfo.getBytes(Charset.forName("UTF-8"));

        // prepare the content server id
        byte[] contentServerIdByte = contentServerId.getBytes(Charset.forName("UTF-8"));

        // write feed content
        File inputFile = new File(inputFilename);
        byte[] feedContentByte = null;
        if (inputFile.length() != 0) {
            String xMLFilename = XMLCreator.createXML(inputFile, contentServerId);
            File xMLFile = new File(xMLFilename);

            try {
                FileInputStream fis = new FileInputStream(xMLFile);
                feedContentByte = new byte[(int) xMLFile.length()];
                fis.read(feedContentByte);
                fis.close();
                xMLFile.delete();
            } catch (IOException e1) {
                System.out.println("ContentServer failed to read the XML temp file.");
                e1.printStackTrace();
            }
        } else {
            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(inputFile);
                feedContentByte = new byte[(int) inputFile.length()];
                fileInputStream.read(feedContentByte);
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            dataOutputStream.writeInt(headerFirstLineByte.length);
            dataOutputStream.write(headerFirstLineByte);
            dataOutputStream.writeInt(headerSecondLineByte.length);
            dataOutputStream.write(headerSecondLineByte);
            dataOutputStream.writeInt(headerThirdLineByte.length);
            dataOutputStream.write(headerThirdLineByte);
            dataOutputStream.writeInt(lamportClockInfoByte.length);
            dataOutputStream.write(lamportClockInfoByte);
            dataOutputStream.writeInt(contentServerIdByte.length);
            dataOutputStream.write(contentServerIdByte);
            dataOutputStream.writeInt(feedContentByte == null ? 0 : feedContentByte.length);
            dataOutputStream.write(feedContentByte);
        } catch (IOException e) {
            System.out.println("ContentServer failed to send put feed request.");
            System.out.println("Detail: ");
            e.printStackTrace();
        }
    }

    private static void receiveServerResponse(DataInputStream dataInputStream) {
        int responseFirstLineLength;
        int responseSecondLineLength;
        try {
            responseFirstLineLength = dataInputStream.readInt();
            byte[] responseFirstLineByte = new byte[responseFirstLineLength];
            dataInputStream.readFully(responseFirstLineByte, 0, responseFirstLineLength);
            String responseFirstLine = new String(responseFirstLineByte);
            System.out.println("[ContentServer:" + contentServerId + "]: " + responseFirstLine);

            String reCode = responseFirstLine.split(" ", 3)[1];

            if (reCode.equals("200") || reCode.equals("201")) {
                responseSecondLineLength = dataInputStream.readInt();
                byte[] responseSecondLineByte = new byte[responseSecondLineLength];
                dataInputStream.readFully(responseSecondLineByte, 0, responseSecondLineLength);
                String responseSecondLine = new String(responseSecondLineByte);
                System.out.println("[ContentServer:" + contentServerId + "]: " + responseSecondLine);

                int responseLamportClockLength = dataInputStream.readInt();
                byte[] responseLamportClockByte = new byte[responseLamportClockLength];
                dataInputStream.readFully(responseLamportClockByte, 0, responseLamportClockLength);
                String responseLamportClock = new String(responseLamportClockByte);
                String[] tempStrings = responseLamportClock.split(": ");
                int newTime = Integer.parseInt(tempStrings[1]);
                lamportClock.update(newTime);
            } else {
                System.exit(-1);
            }

        } catch (IOException e) {
            System.out.println("ContentServer failed to receive aggregation server's response.");
            e.printStackTrace();
        }

    }
}