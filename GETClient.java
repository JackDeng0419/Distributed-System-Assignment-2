import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Scanner;

/**
 * contentServer
 * read the command line to find the server name and port number (in URL format)
 * and will send a GET request for the ATOM feed
 */
public class GETClient {

    private static String SERVER_IP;
    private static int SERVER_PORT;

    public static void main(String[] args) throws IOException {

        // receive user input
        String URL = args[0]; 

        // get the domain and port
        String[] domainPort = URL.split(":", 2);
        SERVER_IP = domainPort[0];
        SERVER_PORT = Integer.parseInt(domainPort[1]);

        Socket server = new Socket(SERVER_IP, SERVER_PORT);

        // sending the get request
        DataOutputStream dataOutputStream = new DataOutputStream(server.getOutputStream());


        // write the GET header
        String headerFirstLine = "GET /getFeed HTTP/1.1";
        String headerSecondLine = "Host: 127.0.0.1:9090";
        String headerThirdLine = "Accept: application/xml";
        byte[] headerFirstLineByte = headerFirstLine.getBytes(Charset.forName("UTF-8"));
        byte[] headerSecondLineByte = headerSecondLine.getBytes(Charset.forName("UTF-8"));
        byte[] headerThirdLineByte = headerThirdLine.getBytes(Charset.forName("UTF-8"));

        dataOutputStream.writeInt(headerFirstLineByte.length);
        dataOutputStream.write(headerFirstLineByte);
        dataOutputStream.writeInt(headerSecondLineByte.length);
        dataOutputStream.write(headerSecondLineByte);
        dataOutputStream.writeInt(headerThirdLineByte.length);
        dataOutputStream.write(headerThirdLineByte);

        // receiving the response from the aggregation server
        InputStreamReader in = new InputStreamReader(server.getInputStream());
        BufferedReader receiver = new BufferedReader(in);
        String str = "";
        while ((str = receiver.readLine()) != null) {
            System.out.println(str);
        }
    }
}