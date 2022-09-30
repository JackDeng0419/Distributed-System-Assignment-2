import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class LamportClock {

    private int time;
    private String id;
    private String lamportClockFolder; // the folder of the lamport clock txt file

    public LamportClock() {
        this.time = 0;
    }

    public LamportClock(String id, int hostType) {
        this.time = 0;
        this.id = id;

        if (hostType == Constant.HOST_TYPE_GET_CLIENT) {
            // client
            this.lamportClockFolder = "GETClientLamportClock/";
        } else if (hostType == Constant.HOST_TYPE_CONTENT_SERVER) {
            // content server
            this.lamportClockFolder = "ContentServerLamportClock/";
        }

        File lamportClockFile = new File(lamportClockFolder + id + ".txt");

        try {
            if (lamportClockFile.isFile()) {
                // if the file exists, get the integer in it
                this.time = readInt(lamportClockFile);
            } else {
                // if the file does not exist, create a new file to store the lamport clock
                storeLamportClockToFile();
            }
        } catch (Exception e) {
            System.out.println("Failed to initiate lamport clock for " + id);
            e.printStackTrace();
        }

    }

    public int getTime() {
        return time;
    }

    public void increaseTime() {
        this.time++;
        storeLamportClockToFile();
    }

    public void update(int newTime) {
        this.time = Math.max(time, newTime) + 1;
        storeLamportClockToFile();
    }

    /*
     * This method stores the lamport clock time to a txt file to preserve the time
     */
    public void storeLamportClockToFile() {
        if (lamportClockFolder != null && id != null) {
            File lamportClockFile = new File(lamportClockFolder + id + ".txt");
            try {
                OutputStreamWriter fileOutputStream = new OutputStreamWriter(new FileOutputStream(lamportClockFile),
                        StandardCharsets.UTF_8);
                fileOutputStream.write(String.valueOf(time));
                fileOutputStream.close();
            } catch (Exception e) {
                System.out.println("Failed to initiate lamport clock for " + id);
                e.printStackTrace();
            }
        }
    }

    /*
     * This method reads an integer from the lamport clock txt file
     */
    private int readInt(File inputFile) {

        BufferedReader reader;
        int returnTime = 0;

        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8));
            String line = reader.readLine();
            reader.close();
            returnTime = Integer.parseInt(line);
        } catch (Exception e) {
            System.out.println("Failed to read int from the lamport clock file.");
        }

        return returnTime;
    }

}
