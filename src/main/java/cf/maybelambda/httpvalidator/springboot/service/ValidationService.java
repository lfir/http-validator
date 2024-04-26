package cf.maybelambda.httpvalidator.springboot.service;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
    static final String HEADER_KEY_VALUE_DELIMITER = ":";
    private HttpClient client;
    private static Logger logger = LoggerFactory.getLogger(ValidationService.class);
    @Autowired
    private EmailNotificationService notificationService;
    @Autowired
    private XMLValidationTaskDao taskReader;

    public ValidationService() {
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.ALWAYS).build();
    }

    @Scheduled(cron = "${cron.expression}")
    public void execValidations() {
        List<String[]> failures = new ArrayList<>();
        for (ValidationTask task : this.taskReader.getAll()) {
            HttpRequest.Builder req = HttpRequest.newBuilder();
            task.reqHeaders().forEach(h -> req.headers(h.split(HEADER_KEY_VALUE_DELIMITER)));
            req.GET();

            try {
                HttpResponse<String> res = this.client.send(req.uri(URI.create(task.reqURL())).build(), HttpResponse.BodyHandlers.ofString());
                if (isNull(res.body()) || !task.isValid(res.statusCode(), res.body())) {
                    String[] notifData = { task.reqURL(), String.valueOf(res.statusCode()), res.body() };
                    failures.add(notifData);
                    logger.info("VALIDATION FAILURE for: {}", task.reqURL());
                } else {
                    logger.info("VALIDATION OK for: {}", task.reqURL());
                }
            } catch (IOException | InterruptedException e) {
                logger.error("HTTPClient's request for the validation task could not be completed.", e);
            }
        }

        if (failures.size() > 0) {
            this.notificationService.sendVTaskErrorsNotification(failures);
        }
    }

    void setClient(HttpClient client) { this.client = client; }

    void setNotificationService(EmailNotificationService service) { this.notificationService = service; }

    void setTaskReader(XMLValidationTaskDao taskReader) { this.taskReader = taskReader; }

    void setLogger(Logger logger) { this.logger = logger; }
}
