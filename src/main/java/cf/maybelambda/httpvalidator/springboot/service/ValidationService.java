package cf.maybelambda.httpvalidator.springboot.service;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
        List<HttpRequest> reqs = new ArrayList<>();
        List<ValidationTask> tasks = this.taskReader.getAll();
        for (ValidationTask task : tasks) {
            HttpRequest.Builder req = HttpRequest.newBuilder();
            req.uri(URI.create(task.reqURL()));
            task.reqHeaders().forEach(h -> req.headers(h.split(HEADER_KEY_VALUE_DELIMITER)));
            req.GET();
            reqs.add(req.build());
        }

        Map<Integer, HttpResponse<String>> resps = new HashMap<>();
        List<CompletableFuture<HttpResponse<String>>> completableFutures = reqs.stream()
            .map(request -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
            .toList();
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]));
        allFutures.thenRun(() -> {
            for (int i = 0; i < completableFutures.size(); i++) {
                resps.put(i, completableFutures.get(i).join());
            }
        }).exceptionally(ex -> {
            logger.error("HTTPClient's request for the validation task could not be completed.", ex);
            return null;
        }).join();

        List<String[]> failures = new ArrayList<>();
        for (Map.Entry<Integer, HttpResponse<String>> resen : resps.entrySet()) {
            ValidationTask task = tasks.get(resen.getKey());
            String logmsg = "VALIDATION ";
            HttpResponse<String> res = resen.getValue();
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
