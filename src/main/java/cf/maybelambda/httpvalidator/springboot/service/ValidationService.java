package cf.maybelambda.httpvalidator.springboot.service;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.management.modelmbean.XMLParseException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.rmi.ConnectIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;

@Service
public class ValidationService {
    public static final String HEADER_KEY_VALUE_DELIMITER = "|";
    private final Duration TIMEOUT_SECONDS = Duration.ofSeconds(10);
    private HttpClient client;
    private static Logger logger = LoggerFactory.getLogger(ValidationService.class);
    @Autowired
    private EmailNotificationService notificationService;
    @Autowired
    private XMLValidationTaskDao taskReader;

    public ValidationService() {
        this.client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT_SECONDS)
            .followRedirects(HttpClient.Redirect.ALWAYS).build();
    }

    @Scheduled(cron = "${cron.expression}")
    public void execValidations() throws ConnectIOException, XMLParseException {
        List<HttpRequest> reqs = new ArrayList<>();
        List<ValidationTask> tasks = this.taskReader.getAll();
        for (ValidationTask task : tasks) {
            HttpRequest.Builder req = HttpRequest.newBuilder();
            req.uri(URI.create(task.reqURL()));
            task.reqHeaders().forEach(h -> req.headers(h.split(Pattern.quote(HEADER_KEY_VALUE_DELIMITER))));
            req.timeout(TIMEOUT_SECONDS);
            reqs.add(req.build());
        }

        List<HttpResponse<String>> resps = new ArrayList<>();
        List<CompletableFuture<HttpResponse<String>>> completableFutures = reqs.stream()
            .map(request -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
            .toList();
        CompletableFuture<List<HttpResponse<String>>> combinedFutures = CompletableFuture
            .allOf(completableFutures.toArray(new CompletableFuture[0]))
            .thenApply(future -> completableFutures.stream()
                .map(CompletableFuture::join)
                .toList()
            );
        try {
            resps = combinedFutures.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("HTTPClient's request for the validation task could not be completed.", e);
        }

        List<String[]> failures = new ArrayList<>();
        for (int i = 0; i < resps.size(); i++) {
            ValidationTask task = tasks.get(i);
            HttpResponse<String> res = resps.get(i);
            String logmsg = "VALIDATION ";
            if (isNull(res.body()) || !task.isValid(res.statusCode(), res.body())) {
                String[] notifData = {task.reqURL(), String.valueOf(res.statusCode()), res.body()};
                failures.add(notifData);
                logmsg += "FAILURE";
            } else {
                logmsg += "OK";
            }
            logger.info(logmsg + " for: {}", task.reqURL());
        }
        if (failures.size() > 0) {
            this.notificationService.sendVTaskErrorsNotification(failures);
        }
    }

    void setClient(HttpClient client) { this.client = client; }

    void setNotificationService(EmailNotificationService service) { this.notificationService = service; }

    void setTaskReader(XMLValidationTaskDao taskReader) { this.taskReader = taskReader; }

    void setLogger(Logger logger) { ValidationService.logger = logger; }
}
