import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.PriorityBlockingQueue;

public class FileHandler implements Runnable {

    PriorityBlockingQueue<Message> priorityQueue;
    String aggregatedFilename;

    public FileHandler(PriorityBlockingQueue<Message> priorityQueue, String aggregatedFilename) {
        this.priorityQueue = priorityQueue;
        this.aggregatedFilename = aggregatedFilename;
    }

    @Override
    public void run() {
        // while (priorityQueue.isEmpty()) {
        // try {
        // wait();
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }
        // }

        Message message;
        File outputFile = new File("ATOMFeed.txt");
        try (FileOutputStream os = new FileOutputStream(outputFile, true)) {
            while (true) {
                try {
                    message = priorityQueue.take();

                    if (message.operationType == 99) {
                        // content server disconnected
                        System.out.println("content server disconnected.");
                    } else if (message.operationType == 1) {
                        // construct the XML file
                        System.out.println("Writing the file...");
                        os.write(("<" + message.contentServerId + ">" + "\n").getBytes(Charset.forName("UTF-8")));
                        os.write(message.payload);
                        os.write(("<" + message.contentServerId + ">" + "\n").getBytes(Charset.forName("UTF-8")));

                        // construct the XML file based on the txt file
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
