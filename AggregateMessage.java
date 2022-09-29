public class AggregateMessage {

    public int operationType; // 1: PUT content; 99: Content server disconnected;
    public String contentServerId;
    public Feed feed;

    public AggregateMessage(int operationType, String contentServerId, Feed feed) {
        this.contentServerId = contentServerId;
        this.operationType = operationType;
        this.feed = feed;
    }

    public AggregateMessage(int operationType, String contentServerId) {
        this.contentServerId = contentServerId;
        this.operationType = operationType;
    }
}
