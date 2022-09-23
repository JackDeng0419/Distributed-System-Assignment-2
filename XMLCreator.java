import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLCreator {

    public XMLCreator() {

    }

    // return the filename of the XML file with the xml suffix
    public static String createXML(File inputFile, String xmlFilename) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();

            /*
             * Construct the XML DOM tree
             */

            // Create the root node
            Element feedElement = document.createElement("feed");

            // Create a feed object
            Feed feed = new Feed(inputFile);

            // append child to the feed element
            appendFeedChild(document, feedElement, feed);

            // append root node to the document
            document.appendChild(feedElement);

            /*
             * Generate the XML file
             */

            FileOutputStream outputFile = new FileOutputStream(xmlFilename + ".xml");
            writeXML(document, outputFile);

            return (xmlFilename + ".xml");
        } catch (ParserConfigurationException | FileNotFoundException | TransformerException e) {
            e.printStackTrace();
            return "Create XML failed";
        }

    }

    private static void writeXML(Document document, OutputStream outputFile) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        // Set the output intent to true
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        // transform the DOM tree to the XML
        transformer.transform(new DOMSource(document), new StreamResult(outputFile));
    }

    private static void appendFeedChild(Document document, Element feedElement, Feed feed) {
        // create the title node
        Element titleElement = document.createElement("title");
        titleElement.setTextContent(feed.getTitle());
        feedElement.appendChild(titleElement);

        // create the subtitle
        Element subtitleElement = document.createElement("subtitle");
        subtitleElement.setTextContent(feed.getSubtitle());
        feedElement.appendChild(subtitleElement);

        // create the link
        Element linkElement = document.createElement("link");
        linkElement.setTextContent(feed.getLink());
        feedElement.appendChild(linkElement);

        // create the updated
        Element updatedElement = document.createElement("updated");
        updatedElement.setTextContent(feed.getUpdated());
        feedElement.appendChild(updatedElement);

        // create the author
        Element authorElement = document.createElement("author");
        Element authorNameElement = document.createElement("name");
        authorNameElement.setTextContent(feed.getAuthor());
        authorElement.appendChild(authorNameElement);
        feedElement.appendChild(authorElement);

        // create the id
        Element idElement = document.createElement("id");
        idElement.setTextContent(feed.getId());
        feedElement.appendChild(idElement);

        // create the entries
        ArrayList<FeedEntry> entries = feed.getEntries();
        for (FeedEntry feedEntry : entries) {
            Element entryElement = document.createElement("entry");

            // create the entry's title
            Element entryTitleElement = document.createElement("title");
            entryTitleElement.setTextContent(feedEntry.getTitle());
            entryElement.appendChild(entryTitleElement);

            // create the entry's link
            Element entryLinkElement = document.createElement("link");
            entryLinkElement.setTextContent(feedEntry.getLink());
            entryElement.appendChild(entryLinkElement);

            // create the entry's id
            Element entryIdElement = document.createElement("id");
            entryIdElement.setTextContent(feedEntry.getId());
            entryElement.appendChild(entryIdElement);

            // create the entry's updated
            Element entryUpdatedElement = document.createElement("updated");
            entryUpdatedElement.setTextContent(feedEntry.getUpdated());
            entryElement.appendChild(entryUpdatedElement);

            // create the entry's author
            if (feedEntry.getAuthor() != null) {
                Element entryAuthorElement = document.createElement("author");
                Element entryAuthorNameElement = document.createElement("name");
                entryAuthorNameElement.setTextContent(feedEntry.getAuthor());
                entryAuthorElement.appendChild(entryAuthorNameElement);
                entryElement.appendChild(entryAuthorElement);
            }

            // create the entry's summary
            Element entrySummaryElement = document.createElement("summary");
            entrySummaryElement.setTextContent(feedEntry.getSummary());
            entryElement.appendChild(entrySummaryElement);

            feedElement.appendChild(entryElement);
        }
    }
}
