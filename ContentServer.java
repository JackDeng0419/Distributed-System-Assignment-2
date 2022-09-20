import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * contentServer 
 */
public class ContentServer {
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

        // sending the put request
        PrintWriter out = new PrintWriter(server.getOutputStream());
        out.println("PUT /putContent HTTP/1.1");
        out.println("Host: 127.0.0.1:9090");
        out.println("Accept: */*");
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