
/* 
 * This class is to store the information of the entry item in a feed.
 */
public class FeedEntry {
    private String title;
    private String link;
    private String id;
    private String updated;
    private String author;
    private String summary;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public boolean isATOMXMLFormat() {

        if (isEmptyString(title) || isEmptyString(link) || isEmptyString(id) || isEmptyString(updated)
                || isEmptyString(summary)) {
            return false;
        }

        return true;
    }

    private boolean isEmptyString(String str) {
        return str == null || str.isEmpty() || str.trim().isEmpty();
    }
}
