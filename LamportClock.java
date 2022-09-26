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
    private int hostType;
    private String lamportClockFolder = null;

    public LamportClock() {
        this.time = 0;
    }

    public LamportClock(String id, int hostType) {
        this.time = 0;
        this.id = id;
        this.hostType = hostType;

        if (hostType == GeneralDefinition.HOST_TYPE_GET_CLIENT) {
            this.lamportClockFolder = "GETClientLamportClock/";
        } else if (hostType == GeneralDefinition.HOST_TYPE_CONTENT_SERVER) {
            this.lamportClockFolder = "ContentServerLamportClock/";
        }

        File lamportClockFile = new File(lamportClockFolder + id + ".txt");

        try {
            if (lamportClockFile.isFile()) {
                this.time = readInt(lamportClockFile);
            } else {
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

    public void storeLamportClockToFile() {
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
