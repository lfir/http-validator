package cf.maybelambda.httpvalidator.springboot.persistence;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
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
    @Value("${datafile}")
    private String DATAFILE_PATH;
    private DocumentBuilder xmlParser;
    private static final Logger logger = LoggerFactory.getLogger(XMLValidationTaskDao.class);

    public XMLValidationTaskDao() {
        try {
            this.xmlParser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            logger.error("XML parser {} could not be initialized.", DocumentBuilder.class.getSimpleName());
            throw new RuntimeException(e);
        }
    }

    public List<ValidationTask> getAll() {
        List<ValidationTask> tasks = new ArrayList<>();
        try {
            File datafile = new File(DATAFILE_PATH);
            Document doc = this.xmlParser.parse(datafile);
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
            logger.error("Failed to parse datafile at: {}", DATAFILE_PATH);
            throw new RuntimeException(e);
        }
        return tasks;
    }

    void setXmlParser(DocumentBuilder xmlParser) { this.xmlParser = xmlParser; }
}
