package cf.maybelambda.httpvalidator.springboot.service;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

@Service
public class ValidationService {
    @Value("${datafile}")
    private String DATAFILE_PATH;
    private List<ValidationTask> tasks;
    private DocumentBuilder xmlParser;
    private HttpClient client;
    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    public ValidationService() {
        try {
            this.xmlParser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS).build();
        } catch (ParserConfigurationException e) {
            logger.error("XML parser {} could not be initialized.", DocumentBuilder.class.getSimpleName());
            throw new RuntimeException(e);
        }
    }

    protected void readTasksFromDataFile() {
        this.tasks = new ArrayList<>();

        try {
            File datafile = new File(DATAFILE_PATH);
            Document doc = this.xmlParser.parse(datafile);
            NodeList validations = doc.getElementsByTagName("validation");

            for (int i = 0; i < validations.getLength(); i++) {
                NamedNodeMap nm = validations.item(i).getAttributes();
                ValidationTask v = new ValidationTask(
                    Integer.parseInt(nm.getNamedItem("reqmethod").getTextContent()),
                    nm.getNamedItem("requrl").getTextContent(),
                    nm.getNamedItem("reqbody").getTextContent(),
                    Integer.parseInt(nm.getNamedItem("ressc").getTextContent()),
                    nm.getNamedItem("resbody").getTextContent()
                );
                this.tasks.add(v);
            }
        } catch (NullPointerException | IOException | SAXException e) {
            logger.error("Failed to parse datafile at: {}", DATAFILE_PATH, e);
        }
    }

    @Scheduled(cron = "${cron.expression}")
    public void execValidations() {
        this.readTasksFromDataFile();

        for (ValidationTask task : this.tasks) {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(task.reqURL())).GET().build();
            if (0 != task.reqMethod()) {
                // TODO: POST case
            }

            try {
                HttpResponse<String> res = this.client.send(req, HttpResponse.BodyHandlers.ofString());
                if (isNull(res.body()) || !task.isValid(res.statusCode(), res.body())) {
                    // TODO: send notifications
                    logger.info("VALIDATION FAILURE for: {}", task.reqURL());
                } else {
                    logger.info("VALIDATION OK for: {}", task.reqURL());
                }
            } catch (IOException | InterruptedException e) {
                logger.error("HTTPClient's request for the validation task could not be completed.", e);
            }
        }
    }

    protected List<ValidationTask> getTasks() {
        return this.tasks;
    }

    protected void setXmlParser(DocumentBuilder xmlParser) {
        this.xmlParser = xmlParser;
    }

    protected void setClient(HttpClient client) {
        this.client = client;
    }
}
