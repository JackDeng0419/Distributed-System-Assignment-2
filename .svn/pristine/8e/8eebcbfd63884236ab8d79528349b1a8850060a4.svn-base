import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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
        Scanner myObj = new Scanner(System.in); 
        System.out.println("Enter the URL: ");
        String URL = myObj.nextLine(); 

        // get the domain and port
        String[] domainPort = URL.split(":", 2);
        SERVER_IP = domainPort[0];
        SERVER_PORT = Integer.parseInt(domainPort[1]);

        Socket server = new Socket(SERVER_IP, SERVER_PORT);

        // sending the get request
        PrintWriter out = new PrintWriter(server.getOutputStream());
        out.println("GET /getFeed HTTP/1.1");
        out.println("Host: 127.0.0.1:9090");
        out.println("Accept: application/xml");
        out.flush();

        // receiving the response from the aggregation server
        InputStreamReader in = new InputStreamReader(server.getInputStream());
        BufferedReader receiver = new BufferedReader(in);
        String str = "";
        while ((str = receiver.readLine()) != null) {
            System.out.println(str);
        }
    }
}