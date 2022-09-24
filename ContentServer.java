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
import java.util.Timer;
import java.util.UUID;

import javax.swing.text.StyledEditorKit.BoldAction;

/**
 * contentServer
 */
public class ContentServer {
    private static String SERVER_IP;
    private static int SERVER_PORT;
    private static UUID uuid;
    private static boolean isFirstTimeSending = true;

    public static void main(String[] args) throws IOException, InterruptedException {
        // generate an unique id for this content server
        uuid = UUID.randomUUID();

        // receive URL from user input
        Scanner URLScanner = new Scanner(System.in);
        System.out.println("Enter URL: ");
        String URL = URLScanner.nextLine();

        // get the domain and port
        String[] domainPort = URL.split(":", 2);
        SERVER_IP = domainPort[0];
        SERVER_PORT = Integer.parseInt(domainPort[1]);

        while (true) {
            // receive filename from user input
            Scanner filenameScanner = new Scanner(System.in);
            System.out.println("Enter filename (press enter will upload the file): ");
            String filename = filenameScanner.nextLine();
            // URLScanner.close();
            // filenameScanner.close();

            Socket server = new Socket(SERVER_IP, SERVER_PORT);
            DataOutputStream dataOutputStream = new DataOutputStream(server.getOutputStream());

            FileInputStream fis = null;

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

                dataOutputStream.writeInt(headerFirstLineByte.length);
                dataOutputStream.write(headerFirstLineByte);
                dataOutputStream.writeInt(headerSecondLineByte.length);
                dataOutputStream.write(headerSecondLineByte);
                dataOutputStream.writeInt(headerThirdLineByte.length);
                dataOutputStream.write(headerThirdLineByte);

                // write content server id
                String contentServerId = uuid.toString();
                byte[] contentServerIdByte = contentServerId.getBytes(Charset.forName("UTF-8"));

                dataOutputStream.writeInt(contentServerIdByte.length);
                dataOutputStream.write(contentServerIdByte);

                // write feed content
                File inputFile = new File(filename);

                // create the xml file from the input file
                // Feed feed = new Feed(inputFile);

                String xMLFilename = XMLCreator.createXML(inputFile, contentServerId);

                // read the XML file
                File xMLFile = new File(xMLFilename);
                fis = new FileInputStream(xMLFile);
                byte[] feedContentByte = new byte[(int) xMLFile.length()];
                fis.read(feedContentByte);

                // output the myByteArray to the aggregation server
                dataOutputStream.writeInt(feedContentByte.length);
                dataOutputStream.write(feedContentByte);

                // receiving the response from the aggregation server
                InputStreamReader in = new InputStreamReader(server.getInputStream());
                BufferedReader receiver = new BufferedReader(in);
                String str = "";
                while ((str = receiver.readLine()) != null) {
                    System.out.println(str);
                }
                xMLFile.delete();
                fis.close();
                dataOutputStream.close();

                if (isFirstTimeSending) {
                    Timer timer = new Timer();
                    timer.schedule(new HeartBeatSender(contentServerId, SERVER_IP, SERVER_PORT), 6000L, 9000L);
                }

                isFirstTimeSending = false;
            } catch (Exception e) {
                System.out.println("error: " + e);
            }
        }
    }

}