import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Deque;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/* 
 * This class is responsible for creating the XML file
 */
public class XMLCreator {

    private static String AggregationServerXML = Constant.AGGREGATION_SERVER_XML_FILENAME;

    public XMLCreator() {

    }

    /*
     * inputFile: the txt input feed file
     * xmlFilename: the filename of the output xml file
     * 
     * return: the filename of the XML file with the xml suffix
     * 
     * This method is used by the content server to change the txt feed file into a
     * xml file
     */
    public static String createXML(File inputFile, String xmlFilename) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            // Create a feed object
            Feed feed = new Feed(inputFile);
            builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();

            /*
             * Construct the XML DOM tree
             */
            // Create the root node
            Element feedElement = document.createElement("feed");
            feedElement.setAttribute("xml:lang", "en-US");
            feedElement.setAttribute("xmlns", "http://www.w3.org/2005/Ato");

            // append child to the feed element
            appendFeedChild(document, feedElement, feed);

            // append root node to the document
            document.appendChild(feedElement);

            /*
             * Generate the XML file
             */
            writeXML(document, new File(xmlFilename + ".xml"));

            return (xmlFilename + ".xml");
        } catch (ParserConfigurationException | FileNotFoundException | TransformerException e) {
            System.out.println("XMLCreator failed to create XML file from the input file.");
            e.printStackTrace();
            return "Create XML failed";
        }

    }

    /*
     * feedQueue: the feed queue object that aggregates the feeds
     * 
     * This method is used by the aggregation server to create the XML aggregation
     * file from the feed queue
     */
    public static void createXML(Deque<Feed> feedQueue) {

        File outputFile = new File(AggregationServerXML);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            Element root = document.createElement("feeds");

            /*
             * Construct the XML DOM tree
             */
            for (Feed feed : feedQueue) {

                // Create the feed node
                Element feedElement = document.createElement("feed");
                feedElement.setAttribute("xml:lang", "en-US");
                feedElement.setAttribute("xmlns", "http://www.w3.org/2005/Ato");
                feedElement.setAttribute("CSID", feed.getContentServerId());

                // append child to the feed element
                appendFeedChild(document, feedElement, feed);

                // append root node to the document
                root.appendChild(feedElement);
            }

            /*
             * Generate the XML file
             */
            document.appendChild(root);

            writeXML(document, outputFile);

        } catch (ParserConfigurationException | TransformerException e) {

            e.printStackTrace();
        }
    }

    /*
     * document: the XML document object
     * outputFile: the output XML file
     * 
     * This method write the XML object to an XML file
     */
    private static void writeXML(Document document, File outputFile) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        // Set the output property
        document.setXmlStandalone(true);
        transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "iso-8859-1");

        // transform the DOM tree to XML and write to the output file
        transformer.transform(new DOMSource(document), new StreamResult(outputFile));
    }

    /*
     * document: the XML document object
     * feedElement: the XML feed element to append
     * feed: the feed object containing the feed information
     * 
     * This method append the feed information to the feed node
     */
    private static void appendFeedChild(Document document, Element feedElement, Feed feed) {
        // create the title node
        createNewNodeAndAppend(document, "title", feed.getTitle(), feedElement);

        // create the subtitle
        createNewNodeAndAppend(document, "subtitle", feed.getSubtitle(), feedElement);

        // create the link
        createNewNodeAndAppend(document, "link", feed.getLink(), feedElement);

        // create the updated
        createNewNodeAndAppend(document, "updated", feed.getUpdated(), feedElement);

        // create the author
        Element authorElement = createNewNodeAndAppend(document, "author", null, feedElement);
        createNewNodeAndAppend(document, "name", feed.getAuthor(), authorElement);

        // create the id
        createNewNodeAndAppend(document, "id", feed.getId(), feedElement);

        // create the entries
        ArrayList<FeedEntry> entries = feed.getEntries();
        for (FeedEntry feedEntry : entries) {
            Element entryElement = createNewNodeAndAppend(document, "entry", null, feedElement);
            appendFeedEntry(document, entryElement, feedEntry);
        }
    }

    /*
     * document: the xml document object
     * entryElement: the XML entry element to append
     * feedEntry: the FeedEntry object containing the entry information
     * 
     * This method appends the feed entry information to the entry node
     */
    private static void appendFeedEntry(Document document, Element entryElement, FeedEntry feedEntry) {
        // create the entry's title
        createNewNodeAndAppend(document, "title", feedEntry.getTitle(), entryElement);

        // create the entry's link
        createNewNodeAndAppend(document, "link", feedEntry.getLink(), entryElement);

        // create the entry's id
        createNewNodeAndAppend(document, "id", feedEntry.getId(), entryElement);

        // create the entry's updated
        createNewNodeAndAppend(document, "updated", feedEntry.getUpdated(), entryElement);

        // create the entry's author
        if (feedEntry.getAuthor() != null) {
            Element entryAuthorElement = createNewNodeAndAppend(document, "author", null, entryElement);
            createNewNodeAndAppend(document, "name", feedEntry.getTitle(), entryAuthorElement);
        }

        // create the entry's summary
        createNewNodeAndAppend(document, "summary", feedEntry.getSummary(), entryElement);
    }

    /*
     * document: the XML document object
     * nodeName: the tag name of the xml node
     * nodeContent: the text content of the new node
     * parentElement: the parent element to which the new node is appended
     * 
     * This method creates a new node and append it to the parent node
     */
    private static Element createNewNodeAndAppend(Document document, String tagName, String nodeContent,
            Element parentElement) {
        Element newNode = document.createElement(tagName);

        if (nodeContent != null) {
            newNode.setTextContent(nodeContent);
        }
        parentElement.appendChild(newNode);
        return newNode;
    }
}
