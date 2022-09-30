import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/* 
 * This class is used for parsing the XML file into a feed object or a feed queue
 */
public class XMLParser {

    /*
     * inputFile: the input XML file
     * 
     * return: the feed object parsed from the input file
     * 
     * This method parses the input xml file and generate a feed object, mainly used
     * by the aggregation server to parse the XML feed sent by the content server
     */
    public static Feed parseXMLFile(File inputFile) {
        try {
            // get a document element
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            // extract the feed information from the XML file
            Element feedElement = doc.getDocumentElement();
            String title = feedElement.getElementsByTagName("title").item(0).getTextContent();
            String subtitle = feedElement.getElementsByTagName("subtitle").item(0).getTextContent();
            String link = feedElement.getElementsByTagName("link").item(0).getTextContent();
            String updated = feedElement.getElementsByTagName("updated").item(0).getTextContent();
            Element authorElement = (Element) feedElement.getElementsByTagName("author").item(0);
            String author = authorElement.getElementsByTagName("name").item(0).getTextContent();
            String id = feedElement.getElementsByTagName("id").item(0).getTextContent();

            // extract the feed entry information and construct the FeedEntry object
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

            // construct the Feed object
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

    /*
     * inputFile: the input XML file
     * 
     * return: the feed queue parsed from the input file
     * 
     * This method parses the input xml file and generate a feed queue, mainly used
     * by the aggregation server recover the feed queue
     */
    public static LinkedList<Feed> getFeedQueueFromAggregatedXML(File inputFile) {
        LinkedList<Feed> feedQueue = new LinkedList<>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;

        try {
            // get the document element
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            Element feedsElement = doc.getDocumentElement();

            // get the list of feed nodes of the XML file
            NodeList feedElements = feedsElement.getElementsByTagName("feed");

            for (int i = 0; i < feedElements.getLength(); i++) {
                /* extract the feed information for each feed */

                Element feedElement = (Element) feedElements.item(i);

                // construct the Feed object
                Feed feed = new Feed();

                // set the feed attributes
                setFeedFromFeedElement(feed, feedElement);

                feedQueue.push(feed);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        return feedQueue;
    }

    /*
     * This method sets the Feed object attributes based on the information extracted 
     */
    private static void setFeedFromFeedElement(Feed feed, Element feedElement) {

        feed.setTitle(feedElement.getElementsByTagName("title").item(0).getTextContent());
        feed.setSubtitle(feedElement.getElementsByTagName("subtitle").item(0).getTextContent());
        feed.setLink(feedElement.getElementsByTagName("link").item(0).getTextContent());
        feed.setUpdated(feedElement.getElementsByTagName("updated").item(0).getTextContent());
        feed.setId(feedElement.getElementsByTagName("id").item(0).getTextContent());
        feed.setContentServerId(feedElement.getAttribute("CSID"));

        Element authorElement = (Element) feedElement.getElementsByTagName("author").item(0);
        feed.setAuthor(authorElement.getElementsByTagName("name").item(0).getTextContent());

        NodeList entriesNodeList = feedElement.getElementsByTagName("entry");
        ArrayList<FeedEntry> entries = new ArrayList<>();

        // extract the feed entry information for each feed entry node and construct the
        // FeedEntry object
        for (int j = 0; j < entriesNodeList.getLength(); j++) {
            FeedEntry entry = new FeedEntry();
            Element entryElement = (Element) entriesNodeList.item(j);

            entry.setTitle(entryElement.getElementsByTagName("title").item(0).getTextContent());
            entry.setLink(entryElement.getElementsByTagName("link").item(0).getTextContent());
            entry.setId(entryElement.getElementsByTagName("id").item(0).getTextContent());
            entry.setUpdated(entryElement.getElementsByTagName("updated").item(0).getTextContent());
            entry.setSummary(entryElement.getElementsByTagName("summary").item(0).getTextContent());

            Element entryAuthorElement = (Element) entryElement.getElementsByTagName("author").item(0);
            if (entryAuthorElement != null) {
                entry.setAuthor(entryAuthorElement.getElementsByTagName("name").item(0).getTextContent());
            }

            entries.add(entry);
        }

        feed.setEntries(entries);

    }
}
