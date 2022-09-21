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
        while (true) {
            Message message;
            try {
                message = priorityQueue.take();
                System.out.println("operation type: " + message.operationType);
                System.out.println("content server id: " + message.contentServerId);
                System.out.println("payload: " + message.payload.toString());
                if (message.operationType == 99) {
                    // content server disconnected
                    System.out.println("content server disconnected.");
                } else if (message.operationType == 1) {
                    // construct the XML file
                    System.out.println("Writing the XML file...");

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
