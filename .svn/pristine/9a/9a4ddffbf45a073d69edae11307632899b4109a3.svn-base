import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ContentServerHandler implements Runnable {

    private Socket client;
    private OutputStream out;
    public final static String FILE_PATH_NAME = "./ATOMFeed.txt";

    public ContentServerHandler(Socket clientSocket) throws IOException {
        this.client = clientSocket;
        out = client.getOutputStream();
    }

    @Override
    public void run() {

        int fileSize = 1024;

        try {
            System.out.println("Saving new content to " + FILE_PATH_NAME.substring(2) + "(" + fileSize + " bytes)");

            // output the myByteArray to the client
            PrintWriter printWriter = new PrintWriter(out);
            printWriter.println("New content is saved.");
            printWriter.flush();

            System.out.println("Done");
        } finally {
            try {
                client.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
