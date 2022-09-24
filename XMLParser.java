import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLParser {

    public static Feed parseXMLFile(File inputFile) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            Element feedElement = doc.getDocumentElement();
            String title = feedElement.getElementsByTagName("title").item(0).getTextContent();
            String subtitle = feedElement.getElementsByTagName("subtitle").item(0).getTextContent();
            String link = feedElement.getElementsByTagName("link").item(0).getTextContent();
            String updated = feedElement.getElementsByTagName("updated").item(0).getTextContent();
            Element authorElement = (Element) feedElement.getElementsByTagName("author").item(0);
            String author = authorElement.getElementsByTagName("name").item(0).getTextContent();
            String id = feedElement.getElementsByTagName("id").item(0).getTextContent();

            NodeList entriesNodeList = doc.getElementsByTagName("entry");
            ArrayList<FeedEntry> entries = new ArrayList<>();
            for (int i = 0; i < entriesNodeList.getLength(); i++) {
                FeedEntry entry = new FeedEntry();
                Element entryElement = (Element) entriesNodeList.item(i);
                String entryTitle = entryElement.getElementsByTagName("title").item(0).getTextContent();
                String entryLink = entryElement.getElementsByTagName("link").item(0).getTextContent();
                String entryId = entryElement.getElementsByTagName("id").item(0).getTextContent();
                String entryUpdated = entryElement.getElementsByTagName("updated").item(0).getTextContent();
                Element entryAuthorElement = (Element) entryElement.getElementsByTagName("author").item(0);
                if (entryAuthorElement != null) {
                    String entryAuthor = entryAuthorElement.getElementsByTagName("name").item(0).getTextContent();
                    entry.setAuthor(entryAuthor);
                }
                String entrySummary = entryElement.getElementsByTagName("summary").item(0).getTextContent();

                entry.setTitle(entryTitle);
                entry.setLink(entryLink);
                entry.setId(entryId);
                entry.setUpdated(entryUpdated);
                entry.setSummary(entrySummary);
                entries.add(entry);
            }

            Feed feed = new Feed();
            feed.setTitle(title);
            feed.setSubtitle(subtitle);
            feed.setLink(link);
            feed.setUpdated(updated);
            feed.setAuthor(author);
            feed.setId(id);
            feed.setEntries(entries);
            return feed;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
