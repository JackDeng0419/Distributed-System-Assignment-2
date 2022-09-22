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

        Socket server = new Socket(SERVER_IP, SERVER_PORT);
        DataOutputStream out = new DataOutputStream(server.getOutputStream());

        try {

            // turn the file input stream into buffered input stream

            // read the file content into myByteArray

            // write the PUT header
            String headerFirstLine = "PUT /putContent HTTP/1.1";
            String headerSecondLine = "Host: 127.0.0.1:9090";
            String headerThirdLine = "Accept: */*";
            byte[] headerFirstLineByte = headerFirstLine.getBytes(Charset.forName("UTF-8"));
            byte[] headerSecondLineByte = headerSecondLine.getBytes(Charset.forName("UTF-8"));
            byte[] headerThirdLineByte = headerThirdLine.getBytes(Charset.forName("UTF-8"));

            out.writeInt(headerFirstLineByte.length);
            out.write(headerFirstLineByte);
            out.writeInt(headerSecondLineByte.length);
            out.write(headerSecondLineByte);
            out.writeInt(headerThirdLineByte.length);
            out.write(headerThirdLineByte);

            // write content server id
            String contentServerId = uuid.toString();
            byte[] contentServerIdByte = contentServerId.getBytes(Charset.forName("UTF-8"));

            out.writeInt(contentServerIdByte.length);
            out.write(contentServerIdByte);

            // write feed content
            File file = new File(filename);
            fis = new FileInputStream(file);
            byte[] feedContentByte = new byte[(int) file.length()];
            fis.read(feedContentByte);

            // output the myByteArray to the aggregation server
            out.writeInt(feedContentByte.length);
            out.write(feedContentByte);
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