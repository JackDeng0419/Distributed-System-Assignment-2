import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Scanner;

public class Feed {
    private String title;
    private String subtitle;
    private String link;
    private String updated;
    private String author;
    private String id;
    private ArrayList<FeedEntry> entries;
    private String contentServerId;

    public Feed(File inputFile, String contentServerId) throws FileNotFoundException {
        this.entries = new ArrayList<FeedEntry>();
        this.contentServerId = contentServerId;
        Scanner fileScanner = new Scanner(inputFile);
        while (fileScanner.hasNextLine()) {
            String line = fileScanner.nextLine();
            if (!line.equals("entry")) {
                String[] linePair = line.split(":", 2);
                switch (linePair[0]) {
                    case "title":
                        this.title = linePair[1];
                        break;
                    case "subtitle":
                        this.subtitle = linePair[1];
                        break;
                    case "link":
                        this.link = linePair[1];
                        break;
                    case "updated":
                        this.updated = linePair[1];
                        break;
                    case "author":
                        this.author = linePair[1];
                        break;
                    case "id":
                        this.id = linePair[1];
                        break;
                    default:
                        break;
                }
            } else {
                FeedEntry entry = new FeedEntry();
                while (fileScanner.hasNextLine() && !fileScanner.hasNext("entry")) {
                    line = fileScanner.nextLine();
                    String[] linePair = line.split(":", 2);
                    switch (linePair[0]) {
                        case "title":
                            entry.setTitle(linePair[1]);
                            break;
                        case "link":
                            entry.setLink(linePair[1]);
                            break;
                        case "updated":
                            entry.setId(linePair[1]);
                            break;
                        case "id":
                            entry.setUpdated(linePair[1]);
                            break;
                        case "author":
                            entry.setAuthor(linePair[1]);
                            break;
                        case "summary":
                            entry.setSummary(linePair[1]);
                            break;
                        default:
                            break;
                    }
                }
                entries.add(entry);
            }
        }
        fileScanner.close();
    }

    public Feed(File inputFile) throws FileNotFoundException {
        this(inputFile, null);
    }

    public String getContentServerId() {
        return contentServerId;
    }

    public void setContentServerId(String contentServerId) {
        this.contentServerId = contentServerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<FeedEntry> getEntries() {
        return entries;
    }

    public void setEntries(ArrayList<FeedEntry> entries) {
        this.entries = entries;
    }

}
