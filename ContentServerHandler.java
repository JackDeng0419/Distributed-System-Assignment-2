import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.PriorityBlockingQueue;

public class ContentServerHandler implements Runnable {

    private Socket contentServer;
    private OutputStream out;
    private PriorityBlockingQueue<Message> priorityQueue;
    private DataInputStream dataInputStream;
    public final static String FILE_PATH_NAME = "./ATOMFeed.txt";

    public ContentServerHandler(Socket contentServerSocket, PriorityBlockingQueue<Message> priorityQueue,
            DataInputStream dataInputStream)
            throws IOException {
        this.contentServer = contentServerSocket;
        this.priorityQueue = priorityQueue;
        this.dataInputStream = dataInputStream;
        out = contentServer.getOutputStream();
    }

    @Override
    public void run() {
        String contentServerId;
        byte[] payload;

        try {
            // reading content server id
            int contentServerIdByteLength = dataInputStream.readInt();
            byte[] contentServerIdByte = new byte[contentServerIdByteLength];
            dataInputStream.readFully(contentServerIdByte, 0, contentServerIdByteLength);
            contentServerId = new String(contentServerIdByte);

            // reading payload
            int payloadLength = dataInputStream.readInt();
            payload = new byte[payloadLength];
            dataInputStream.readFully(payload, 0, payloadLength);

            // construct the message object
            System.out.println(payload.toString());
            Message message = new Message(GeneralDefinition.PUT_FEED, contentServerId, payload);

            // add message to the priority queue
            priorityQueue.add(message);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        // output the myByteArray to the content server
        PrintWriter printWriter = new PrintWriter(out);
        printWriter.println("New content is saved.");
        printWriter.flush();

        System.out.println("Done");

        try {
            out.close();
            dataInputStream.close();
            contentServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
