import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
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

        if (!new File(inputFilename).isFile()) {
            System.out.println("[ContentServer:" + contentServerId + "]:Feed file does not exist");
            System.exit(-1);
        }

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

        // write feed content
        File inputFile = new File(inputFilename);
        byte[] feedContentByte = null;
        if (inputFile.length() != 0) {
            /* If the file is not empty, construct a XML from it and send the XML file */
            String xMLFilename = XMLCreator.createXML(inputFile, contentServerId);
            File xMLFile = new File(xMLFilename);

            try {
                FileInputStream fileInputStream = new FileInputStream(xMLFile);
                feedContentByte = new byte[(int) xMLFile.length()];
                fileInputStream.read(feedContentByte);
                fileInputStream.close();
                xMLFile.delete();
            } catch (IOException e1) {
                System.out.println("ContentServer failed to read the XML temp file.");
                e1.printStackTrace();
            }
        } else {
            /* If the file is empty, directly send the file */
            try {
                FileInputStream fileInputStream = new FileInputStream(inputFile);
                feedContentByte = new byte[(int) inputFile.length()];
                fileInputStream.read(feedContentByte);
                fileInputStream.close();
            } catch (IOException e) {
                System.out.println("[ContentServer:" + contentServerId + "]:Feed file does not exist");
                System.exit(-1);
            }
        }

        try {
            HTTPUtils.sendString(dataOutputStream, "PUT /putContent HTTP/1.1");
            HTTPUtils.sendString(dataOutputStream, "Host: " + SERVER_IP + ":" + SERVER_PORT);
            HTTPUtils.sendString(dataOutputStream, "Accept: */*");
            HTTPUtils.sendString(dataOutputStream, "LamportClock: " + lamportClock.getTime());
            HTTPUtils.sendString(dataOutputStream, contentServerId);
            dataOutputStream.writeInt(feedContentByte == null ? 0 : feedContentByte.length);
            dataOutputStream.write(feedContentByte);
        } catch (IOException e) {
            System.out.println("ContentServer failed to send put feed request.");
            System.out.println("Detail: ");
            e.printStackTrace();
        }
    }

    private static void receiveServerResponse(DataInputStream dataInputStream) {
        try {
            String responseFirstLine = HTTPUtils.readString(dataInputStream);
            System.out.println("[ContentServer:" + contentServerId + "]: " + responseFirstLine);

            String reCode = responseFirstLine.split(" ", 3)[1];

            if (reCode.equals("200") || reCode.equals("201")) {
                // read the second line of response
                String responseSecondLine = HTTPUtils.readString(dataInputStream);
                System.out.println("[ContentServer:" + contentServerId + "]: " + responseSecondLine);

                // read the lamport clock
                String[] lamportStrings = HTTPUtils.readString(dataInputStream).split(": ");
                lamportClock.update(Integer.parseInt(lamportStrings[1]));
            } else {
                System.exit(-1);
            }

        } catch (IOException e) {
            System.out.println("ContentServer failed to receive aggregation server's response.");
            e.printStackTrace();
        }

    }
}