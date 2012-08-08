package edu.washington.cs.oneswarm.f2f.sharedservices;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;

import edu.washington.cs.oneswarm.f2f.servicesharing.XML;

public class Parser {
    private ExitNodeRecord tempNode;
    private String tempVal;
    int count = 0;

    private final InputStream in;

    public Parser(InputStream in) {
        this.in = in;
    }

    /**
     * Adds ExitNodes found in this parser's XML input to the DB.
     * 
     * @param max
     *            The maximum number of ExitNodes to Register or, 0 to signal
     *            unlimited.
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public List<ExitNodeRecord> parseAsExitNodeList(final int max)
            throws ParserConfigurationException, SAXException, IOException {
        final List<ExitNodeRecord> list = new LinkedList<ExitNodeRecord>();

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        parser.parse(in, new ExitNodeListHandler(list, max));

        return list;
    }

    private class ExitNodeListHandler extends DefaultHandler {
        private final int max;
        private final List<ExitNodeRecord> exitNodes;
        private final DateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");

        public ExitNodeListHandler(List<ExitNodeRecord> list, int max) {
            this.max = max;
            this.exitNodes = list;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            if (qName.equals(XML.EXIT_NODE)) {
                tempNode = new ExitNodeRecord();
                count++;
            }
        }

        @Override
        public void characters(char ch[], int start, int length) throws SAXException {
            tempVal = new String(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equalsIgnoreCase(XML.EXIT_NODE)) {
                if (max == 0 || count < max) {
                    exitNodes.add(tempNode);
                    count++;
                }
            } else if (qName.equalsIgnoreCase(XML.SERVICE_ID)) {
                tempNode.setId(Long.parseLong(tempVal));
            } else if (qName.equalsIgnoreCase(XML.PUBLIC_KEY)) {
                tempNode.setPublicKey(tempVal);
            } else if (qName.equalsIgnoreCase(XML.NICKNAME)) {
                tempNode.setNickname(tempVal);
            } else if (qName.equalsIgnoreCase(XML.BANDWIDTH)) {
                tempNode.setAdvertizedBandwidth(Integer.parseInt(tempVal));
            } else if (qName.equalsIgnoreCase(XML.EXIT_POLICY)) {
                tempNode.setExitPolicy(tempVal.split(","));
            } else if (qName.equalsIgnoreCase(XML.ONLINE_SINCE)) {
                try {
                    tempNode.setOnlineSinceDate(formatter.parse(tempVal));
                } catch (ParseException e) {
                    throw new IllegalArgumentException(e);
                }
            } else if (qName.equalsIgnoreCase(XML.VERSION)) {
                tempNode.setVersion(tempVal);
            } else if (qName.equalsIgnoreCase(XML.SIGNATURE)) {
                try {
                    tempNode.signature = Base64.decode(tempVal);
                } catch (Base64DecodingException e) {
                    throw new IllegalArgumentException(e);
                }
            } else if (qName.equalsIgnoreCase(XML.EXIT_NODE_LIST)) {
            } else {
                throw new IllegalArgumentException("Unrecognized Tag: " + qName);
            }
        }

        @Override
        public void endDocument() throws SAXException {
            synchronized (exitNodes) {
            }
        }
    }
}
