import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.PriorityBlockingQueue;

public class ContentServerHandler implements Runnable {

    private Socket contentServer;
    private OutputStream out;
    private PriorityBlockingQueue<Message> priorityQueue;
    public final static String FILE_PATH_NAME = "./ATOMFeed.txt";

    public ContentServerHandler(Socket contentServerSocket, PriorityBlockingQueue<Message> priorityQueue)
            throws IOException {
        this.contentServer = contentServerSocket;
        this.priorityQueue = priorityQueue;
        out = contentServer.getOutputStream();
    }

    @Override
    public void run() {

        int fileSize = 1024;

        try {

            // receiving the response from the aggregation server
            InputStreamReader in = new InputStreamReader(contentServer.getInputStream());
            BufferedReader receiver = new BufferedReader(in);
            String str = "";
            while ((str = receiver.readLine()) != null) {
                System.out.println(str);
            }

            // read the feed content and push into ATOMFeed.txt
            Message message = new Message(1, "123456", "payload 1".getBytes(Charset.forName("UTF-8")));

            System.out.println("Saving new content to " + FILE_PATH_NAME.substring(2) + "(" + fileSize + " bytes)");

            priorityQueue.add(message);

            // output the myByteArray to the content server
            PrintWriter printWriter = new PrintWriter(out);
            printWriter.println("New content is saved.");
            printWriter.flush();

            System.out.println("Done");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
                contentServer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
