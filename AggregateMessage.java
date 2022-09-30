public class AggregateMessage {

    public int operationType;
    public String contentServerId;
    public Feed feed;

    /*
     * operationType: 1: PUT content; 99: Content server disconnected;
     * contentServerId: the id of the content server
     * feed: the feed object of the feed sent by the content server
     */
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
