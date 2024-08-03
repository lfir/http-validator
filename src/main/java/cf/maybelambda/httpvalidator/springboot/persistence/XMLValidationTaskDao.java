package cf.maybelambda.httpvalidator.springboot.persistence;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.management.modelmbean.XMLParseException;
import javax.swing.text.html.FormSubmitEvent.MethodType;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Provides methods to interact with the XML data file containing validation tasks.
 * <p>
 * This class is responsible for reading and updating the XML data file,
 * parsing its content, and validating its structure against a predefined schema.
 */
@Component
public class XMLValidationTaskDao {
    static final String URL_TAG = "url";
    static final String RES_TAG = "response";
    static final String REQ_BODY_TAG = "reqbody";
    static final String HEADER_TAG = "header";
    static final String VALIDATION_TAG = "validation";
    static final String REQ_METHOD_ATTR = "method";
    static final String RES_SC_ATTR = "statuscode";
    static final String DATAFILE_PROPERTY = "datafile";
    private static final String SCHEMA_FILENAME = "validations.xsd";
    private DocumentBuilder xmlParser;
    private static Logger logger = LoggerFactory.getLogger(XMLValidationTaskDao.class);

    @Autowired
    private Environment env;
    @Autowired
    private ObjectMapper mapper;

    /**
     * Constructs an instance of XMLValidationTaskDao.
     * <p>
     * Initializes the XML parser with schema validation and security features.
     *
     * @throws ParserConfigurationException if a DocumentBuilder cannot be created.
     * @throws SAXException if an error occurs during schema parsing.
     * @throws IOException if an error occurs during schema file loading.
     */
    public XMLValidationTaskDao() throws ParserConfigurationException, SAXException, IOException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema((new ClassPathResource(SCHEMA_FILENAME)).getURL());
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setSchema(schema);
        dbFactory.setIgnoringElementContentWhitespace(true);
        dbFactory.setNamespaceAware(true);
        dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        this.xmlParser = dbFactory.newDocumentBuilder();

        XMLErrorHandler xsdErrorHandler = new XMLErrorHandler();
        this.xmlParser.setErrorHandler(xsdErrorHandler);
    }

    /**
     * Retrieves the file path of the XML data file from the environment properties.
     *
     * @return The path of the XML data file.
     */
    Path getDataFilePath() { return Path.of(requireNonNull(this.env.getProperty(DATAFILE_PROPERTY))); }

    /**
     * Parses the given XML input stream into a Document.
     *
     * @param inputStream The input stream of the XML content.
     * @return The parsed Document.
     * @throws XMLParseException if parsing fails.
     */
    Document parseXMLInput(InputStream inputStream) throws XMLParseException {
        try {
            return this.xmlParser.parse(inputStream);
        } catch (Exception e) {
            String errmsg = "Failed to parse target XML content";
            logger.error(errmsg, e);
            throw new XMLParseException(e, errmsg + "\n");
        }
    }

    /**
     * Updates the XML data file with the content of the given multipart file.
     *
     * @param file The multipart file containing the new XML content.
     * @throws IOException if an I/O error occurs.
     * @throws NullPointerException if the file is null.
     * @throws XMLParseException if the XML content is invalid.
     */
    public synchronized void updateDataFile(MultipartFile file) throws IOException, NullPointerException, XMLParseException {
        try {
            this.parseXMLInput(file.getInputStream());
            Files.write(this.getDataFilePath(), file.getBytes());
        } catch (NullPointerException | XMLParseException e) {
            logger.warn("Invalid EXTERNAL XML received from API");
            throw e;
        } catch (IOException e) {
            logger.error("Failed writing new datafile to disk", e);
            throw e;
        }
    }

    /**
     * Retrieves the XML Document from the data file.
     *
     * @return The parsed Document.
     * @throws XMLParseException if parsing fails.
     * @throws FileNotFoundException if the data file is not found.
     */
    synchronized Document getDocData() throws XMLParseException, FileNotFoundException {
        return this.parseXMLInput(new FileInputStream(this.getDataFilePath().toFile()));
    }

    /**
     * Retrieves all validation tasks from the XML data file.
     *
     * @return A list of validation tasks.
     * @throws XMLParseException if parsing fails.
     * @throws FileNotFoundException if the data file is not found.
     */
    public List<ValidationTask> getAll() throws XMLParseException, FileNotFoundException {
        List<ValidationTask> tasks = new ArrayList<>();
        NodeList validations = this.getDocData().getElementsByTagName(VALIDATION_TAG);
        for (int i = 0; i < validations.getLength(); i++) {
            NodeList validation = validations.item(i).getChildNodes();
            MethodType method = null;
            String url = null;
            List<String> headers = new ArrayList<>();
            JsonNode reqBody = this.mapper.nullNode();
            Integer resStatusCode = null;
            String resBody = null;

            for (int j = 0; j < validation.getLength(); j++) {
                Node childNode = validation.item(j);
                NamedNodeMap attrs = childNode.getAttributes();
                String name = childNode.getNodeName();
                String content = childNode.getTextContent().trim();

                if (URL_TAG.equals(name)) {
                    int val = Integer.parseInt(attrs.getNamedItem(REQ_METHOD_ATTR).getTextContent());
                    method = MethodType.values()[val];
                    url = content;
                }
                if (HEADER_TAG.equals(name)) {
                    headers.add(content);
                }
                if (REQ_BODY_TAG.equals(name)) {
                    try {
                        reqBody = this.mapper.readTree(content);
                    } catch (JsonProcessingException e) {
                        String errmsg = "Invalid JSON content encountered in the data file";
                        logger.error(errmsg, e);
                        throw new XMLParseException(e, errmsg + "\n");
                    }
                }
                if (RES_TAG.equals(name)) {
                    resStatusCode = Integer.parseInt(attrs.getNamedItem(RES_SC_ATTR).getTextContent());
                    resBody = content;
                }
            }

            tasks.add(
                new ValidationTask(method, url, headers, reqBody, resStatusCode, resBody)
            );
        }
        return tasks;
    }

    /**
     * Checks if the XML data file exists and is readable.
     *
     * @return True if the data file exists and is readable, false otherwise.
     */
    public boolean isDataFileStatusOk() {
        Path path = this.getDataFilePath();
        return Files.isRegularFile(path) && Files.isReadable(path);
    }

    /**
     * Sets the XML parser for the XMLValidationTaskDao. Used for testing purposes.
     *
     * @param xmlParser The XML parser to set.
     */
    void setXmlParser(DocumentBuilder xmlParser) { this.xmlParser = xmlParser; }

    /**
     * Sets the logger; for testing purposes.
     *
     * @param logger The logger to set.
     */
    void setLogger(Logger logger) { XMLValidationTaskDao.logger = logger; }

    /**
     * Sets the environment for the XMLValidationTaskDao.
     * <p>
     * This method is used for testing purposes to inject a mock environment.
     *
     * @param env The environment to set.
     */
    void setEnv(Environment env) { this.env = env; }

    /**
     * Sets the object mapper; for testing purposes.
     *
     * @param mapper The ObjectMapper to set.
     */
    void setObjectMapper(ObjectMapper mapper) { this.mapper = mapper; }
}
