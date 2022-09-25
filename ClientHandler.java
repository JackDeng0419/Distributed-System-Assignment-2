import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import javax.print.DocFlavor.URL;

public class ClientHandler implements Runnable {
    private Socket client;
    private DataOutputStream out;
    public final static String FILE_PATH_NAME = "./ATOMFeed.xml";

    public ClientHandler(Socket clientSocket) throws IOException {
        this.client = clientSocket;
        out = new DataOutputStream(client.getOutputStream());
    }

    @Override
    public void run() {
        FileInputStream fis = null;
        BufferedInputStream bis = null;

        try {
            File file = new File(FILE_PATH_NAME);
            byte[] XMLByte = new byte[(int) file.length()];

            // turn the file input stream into buffered input stream
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);

            // read the file content into myByteArray
            bis.read(XMLByte, 0, XMLByte.length);
            System.out.println("Sending " + FILE_PATH_NAME.substring(2) + "(" + XMLByte.length + " bytes)");

            // write the GET response
            String headerFirstLine = "HTTP/1.1 200 OK";
            byte[] headerFirstLineByte = headerFirstLine.getBytes(Charset.forName("UTF-8"));
            out.writeInt(headerFirstLineByte.length);
            out.write(headerFirstLineByte);

            out.writeInt(XMLByte.length);
            out.write(XMLByte);

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
