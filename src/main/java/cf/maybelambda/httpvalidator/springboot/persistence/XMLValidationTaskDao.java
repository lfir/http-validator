package cf.maybelambda.httpvalidator.springboot.persistence;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.management.modelmbean.XMLParseException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class XMLValidationTaskDao {
    static final String VALIDATION_TAG = "validation";
    static final String REQ_METHOD_ATTR = "reqmethod";
    static final String REQ_URL_ATTR = "requrl";
    static final String REQ_HEADERS_ATTR = "reqheaders";
    static final String HEADER_DELIMITER = ",";
    static final String RES_SC_ATTR = "ressc";
    static final String RES_BODY_ATTR = "resbody";
    private static final String SCHEMA_FILENAME = "validations.xsd";
    @Value("${datafile}")
    private String DATAFILE_PATH;
    private DocumentBuilder xmlParser;
    private static Logger logger = LoggerFactory.getLogger(XMLValidationTaskDao.class);

    public XMLValidationTaskDao() throws ParserConfigurationException, SAXException, IOException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema((new ClassPathResource(SCHEMA_FILENAME)).getURL());
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setSchema(schema);
        dbFactory.setIgnoringElementContentWhitespace(true);
        dbFactory.setNamespaceAware(true);
        this.xmlParser = dbFactory.newDocumentBuilder();

        XMLErrorHandler xsdErrorHandler = new XMLErrorHandler();
        this.xmlParser.setErrorHandler(xsdErrorHandler);
    }

    public List<ValidationTask> getAll() throws XMLParseException {
        List<ValidationTask> tasks = new ArrayList<>();
        try {
            InputStream dfStream = new FileInputStream(DATAFILE_PATH);
            Document doc = this.xmlParser.parse(dfStream);
            NodeList validations = doc.getElementsByTagName(VALIDATION_TAG);

            for (int i = 0; i < validations.getLength(); i++) {
                NamedNodeMap nm = validations.item(i).getAttributes();
                ValidationTask v = new ValidationTask(
                    Integer.parseInt(nm.getNamedItem(REQ_METHOD_ATTR).getTextContent()),
                    nm.getNamedItem(REQ_URL_ATTR).getTextContent(),
                    Arrays.stream(nm.getNamedItem(REQ_HEADERS_ATTR).getTextContent().split(HEADER_DELIMITER)).filter(h -> h != "").toList(),
                    Integer.parseInt(nm.getNamedItem(RES_SC_ATTR).getTextContent()),
                    nm.getNamedItem(RES_BODY_ATTR).getTextContent()
                );
                tasks.add(v);
            }
        } catch (NullPointerException | IOException | SAXException e) {
            String errmsg = "Failed to parse datafile at: " + DATAFILE_PATH;
            logger.error(errmsg);
            throw new XMLParseException(e, errmsg + "\n");
        }
        return tasks;
    }

    void setXmlParser(DocumentBuilder xmlParser) { this.xmlParser = xmlParser; }

    void setLogger(Logger logger) { XMLValidationTaskDao.logger = logger; }
}
