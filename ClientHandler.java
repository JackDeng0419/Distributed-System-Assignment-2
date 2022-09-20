import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import javax.print.DocFlavor.URL;

public class ClientHandler implements Runnable {
    private Socket client;
    private OutputStream out;
    public final static String FILE_PATH_NAME = "./ATOMFeed.txt";

    public ClientHandler(Socket clientSocket) throws IOException {
        this.client = clientSocket;
        out = client.getOutputStream();
    }

    @Override
    public void run() {
        FileInputStream fis = null; 
        BufferedInputStream bis = null;
        
        try {
            File file = new File(FILE_PATH_NAME);
            byte[] myByteArray = new byte[(int) file.length()];


            // turn the file input stream into buffered input stream
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);

            // read the file content into myByteArray
            bis.read(myByteArray, 0, myByteArray.length);
            System.out.println("Sending " + FILE_PATH_NAME.substring(2) + "(" + myByteArray.length + " bytes)");
            
            // output the myByteArray to the client
            out.write(myByteArray, 0, myByteArray.length);
            out.flush();

            System.out.println("Done");
        } catch (IOException e) {
            e.printStackTrace();
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
