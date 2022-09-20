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
import java.util.Scanner;
import java.util.UUID;

/**
 * contentServer
 */
public class ContentServer {
    private static String SERVER_IP;
    private static int SERVER_PORT;
    private static UUID uuid;

    public static void main(String[] args) throws IOException {
        // generate an unique id for this content server
        uuid = UUID.randomUUID();

        // receive user input
        String URL = args[0];
        String filename = args[1];

        // get the domain and port
        String[] domainPort = URL.split(":", 2);
        SERVER_IP = domainPort[0];
        SERVER_PORT = Integer.parseInt(domainPort[1]);

        FileInputStream fis = null;
        BufferedInputStream bis = null;

        Socket server = new Socket(SERVER_IP, SERVER_PORT);
        OutputStream out = server.getOutputStream();

        try {
            File file = new File(filename);
            byte[] myByteArray = new byte[(int) file.length()];

            // turn the file input stream into buffered input stream
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);

            // read the file content into myByteArray
            bis.read(myByteArray, 0, myByteArray.length);
            System.out.println("Sending " + filename + "(" + myByteArray.length + " bytes) to the Aggregation Server.");

            // write the PUT header
            out.write("PUT /putContent HTTP/1.1\n".getBytes(Charset.forName("UTF-8")));
            out.write("Host: 127.0.0.1:9090\n".getBytes(Charset.forName("UTF-8")));
            out.write("Accept: */*\n".getBytes(Charset.forName("UTF-8")));
            out.write(("ContentServerId: " + uuid.toString() + "\n").getBytes(Charset.forName("UTF-8")));

            // output the myByteArray to the aggregation server
            out.write(myByteArray, 0, myByteArray.length);
            out.flush();
        } catch (Exception e) {
            System.out.println("error: " + e);
        }

        // receiving the response from the aggregation server
        InputStreamReader in = new InputStreamReader(server.getInputStream());
        BufferedReader receiver = new BufferedReader(in);
        String str = "";
        while ((str = receiver.readLine()) != null) {
            System.out.println(str);
        }
        server.close();
    }

}