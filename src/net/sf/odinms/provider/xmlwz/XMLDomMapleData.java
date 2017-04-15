package net.sf.odinms.provider.xmlwz;

import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataEntity;
import net.sf.odinms.provider.wz.MapleDataType;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class XMLDomMapleData implements MapleData {
    private final Node node;
    private File imageDataDir;

    public XMLDomMapleData(final FileInputStream fis, final File imageDataDir) {
        try {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            final Document document = documentBuilder.parse(fis);
            this.node = document.getFirstChild();
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
        this.imageDataDir = imageDataDir;
    }

    private XMLDomMapleData(final Node node) {
        this.node = node;
    }

    @Override
    public MapleData getChildByPath(final String path) {
        final String[] segments = path.split("/");
        if (segments[0].equals("..")) {
            return ((MapleData) getParent()).getChildByPath(path.substring(path.indexOf("/") + 1));
        }
        Node myNode = node;
        for (int x = 0; x < segments.length; ++x) {
            final NodeList childNodes = myNode.getChildNodes();
            boolean foundChild = false;
            int numChildNodes;
            try {
                numChildNodes = childNodes.getLength();
            } catch (final NullPointerException npe) {
                //System.err.print("Exception in XMLDomMapleData.getChildByPath() for path " + path + ", " + npe + "\n");
                numChildNodes = 0;
            }
            for (int i = 0; i < numChildNodes; ++i) {
                final Node childNode = childNodes.item(i);
                if (childNode.getNodeType() == Node.ELEMENT_NODE && childNode.getAttributes().getNamedItem("name").getNodeValue().equals(segments[x])) {
                    myNode = childNode;
                    foundChild = true;
                    break;
                }
            }
            if (!foundChild) {
                return null;
            }
        }
        final XMLDomMapleData ret = new XMLDomMapleData(myNode);
        ret.imageDataDir = new File(imageDataDir, getName() + "/" + path).getParentFile();
        return ret;
    }

    @Override
    public List<MapleData> getChildren() {
        final List<MapleData> ret = new ArrayList<>();
        try {
            final NodeList childNodes = node.getChildNodes();
            if (childNodes != null && childNodes.getLength() > 0) {
                for (int i = 0; i < childNodes.getLength(); ++i) {
                    final Node childNode = childNodes.item(i);
                    if (childNode != null && childNode.getNodeType() == Node.ELEMENT_NODE) {
                        final XMLDomMapleData child = new XMLDomMapleData(childNode);
                        child.imageDataDir = new File(imageDataDir, getName());
                        ret.add(child);
                    }
                }
            }
            return ret;
        } catch (final NullPointerException npe) {
            /*
            try {
                System.err.println("NPE in XMLDomMapleData.java in method getChildren(), XML file path: " + imageDataDir.getCanonicalPath());
            } catch (IOException ioe) {
                System.err.println("NPE and IOE in XMLDomMapleData.java in method getChildren()");
            }
            */
            return Collections.emptyList();
        }
    }

    @Override
    public Object getData() {
        final NamedNodeMap attributes = node.getAttributes();
        final MapleDataType type = getType();
        switch (type) {
            case DOUBLE:
            case FLOAT:
            case INT:
            case SHORT:
            case STRING:
            case UOL: {
                final String value = attributes.getNamedItem("value").getNodeValue();
                switch (type) {
                    case DOUBLE:
                        return Double.parseDouble(value);
                    case FLOAT:
                        return Float.parseFloat(value);
                    case INT:
                        return Integer.parseInt(value);
                    case SHORT:
                        return Short.parseShort(value);
                    case STRING:
                    case UOL:
                        return value;
                }
            }
            case VECTOR: {
                final String x = attributes.getNamedItem("x").getNodeValue();
                final String y = attributes.getNamedItem("y").getNodeValue();
                return new Point(Integer.parseInt(x), Integer.parseInt(y));
            }
            case CANVAS: {
                final String width = attributes.getNamedItem("width").getNodeValue();
                final String height = attributes.getNamedItem("height").getNodeValue();
                return new FileStoredPngMapleCanvas(
                    Integer.parseInt(width),
                    Integer.parseInt(height),
                    new File(imageDataDir, getName() + ".png")
                );
            }
        }
        return null;
    }

    @Override
    public MapleDataType getType() {
        final String nodeName = node.getNodeName();
        switch (nodeName) {
            case "imgdir": return MapleDataType.PROPERTY;
            case "canvas": return MapleDataType.CANVAS;
            case "convex": return MapleDataType.CONVEX;
            case "sound":  return MapleDataType.SOUND;
            case "uol":    return MapleDataType.UOL;
            case "double": return MapleDataType.DOUBLE;
            case "float":  return MapleDataType.FLOAT;
            case "int":    return MapleDataType.INT;
            case "short":  return MapleDataType.SHORT;
            case "string": return MapleDataType.STRING;
            case "vector": return MapleDataType.VECTOR;
            case "null":   return MapleDataType.IMG_0x00;
        }
        System.err.println(
            "Returning null in XMLDomMapleData.getType(): node's name: " +
                nodeName +
                ", " +
                "node's URI: " +
                node.getBaseURI()
        );
        return null;
    }

    @Override
    public MapleDataEntity getParent() {
        final Node parentNode = node.getParentNode();
        if (parentNode.getNodeType() == Node.DOCUMENT_NODE) return null;
        final XMLDomMapleData parentData = new XMLDomMapleData(parentNode);
        parentData.imageDataDir = imageDataDir.getParentFile();
        return parentData;
    }

    @Override
    public String getName() {
        return node.getAttributes().getNamedItem("name").getNodeValue();
    }

    @Override
    public Iterator<MapleData> iterator() {
        return getChildren().iterator();
    }
}
