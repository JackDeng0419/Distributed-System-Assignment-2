import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;

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

        try {
            File file = new File(FILE_PATH_NAME);
            byte[] XMLByte = new byte[(int) file.length()];

            // turn the file input stream into buffered input stream
            fis = new FileInputStream(file);

            // read the file content into myByteArray
            fis.read(XMLByte);
            System.out.println("Sending " + FILE_PATH_NAME.substring(2) + "(" + XMLByte.length + " bytes)");

            // write the GET response
            String headerFirstLine = "HTTP/1.1 201 OK";
            byte[] headerFirstLineByte = headerFirstLine.getBytes(Charset.forName("UTF-8"));
            out.writeInt(headerFirstLineByte.length);
            out.write(headerFirstLineByte);
            out.writeInt(XMLByte.length);
            out.write(XMLByte);

            System.out.println("The aggregated feed has been sent.");
            client.close();
            out.close();
        } catch (IOException e) {
            System.out.println("GETClientHandler failed to work.");
            e.printStackTrace();
        }
    }

}
