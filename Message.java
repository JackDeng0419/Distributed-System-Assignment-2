public class Message {

    public int operationType; // 1: PUT content; 99: Content server disconnected;
    public String contentServerId;
    public byte[] payload;

    public Message(int operationType, String contentServerId, byte[] payload) {
        this.contentServerId = contentServerId;
        this.operationType = operationType;
        this.payload = payload;
    }
}
