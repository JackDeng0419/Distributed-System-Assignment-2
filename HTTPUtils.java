import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class HTTPUtils {

    /*
     * dataOutputStream: the data output stream of the receiver's socket
     * str: the string to send
     * 
     * this method sends the string in bytes
     */
    public static void sendString(DataOutputStream dataOutputStream, String str) throws IOException {
        byte[] strByte = str.getBytes(Charset.forName("UTF-8"));
        dataOutputStream.writeInt(strByte.length);
        dataOutputStream.write(strByte);
    }

    /*
     * dataOutputStream: the data output stream of the receiver's socket
     * file: the file to send
     * 
     * this method sends the file in bytes
     */
    public static void sendFile(DataOutputStream dataOutputStream, File file) throws IOException {

        FileInputStream fileInputStream = new FileInputStream(file);

        // read the file content into fileByte
        byte[] fileByte = new byte[(int) file.length()];
        fileInputStream.read(fileByte);
        fileInputStream.close();

        dataOutputStream.writeInt(fileByte.length);
        dataOutputStream.write(fileByte);
    }

    /*
     * dataInputStream: the data input stream of the sender's socket
     * 
     * return: a string from the input stream
     * 
     * this method read bytes and transform them into a string
     */
    public static String readString(DataInputStream dataInputStream) throws IOException {
        int strByteLength = dataInputStream.readInt();
        byte[] strByte = new byte[strByteLength];
        dataInputStream.readFully(strByte);
        return new String(strByte);
    }

}
