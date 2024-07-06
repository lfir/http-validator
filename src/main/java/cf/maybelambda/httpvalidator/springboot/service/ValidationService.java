package cf.maybelambda.httpvalidator.springboot.service;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import javax.management.modelmbean.XMLParseException;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.rmi.ConnectIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static cf.maybelambda.httpvalidator.springboot.HTTPValidatorWebApp.RUN_SCHEDULE_PROPERTY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.START_TIME_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TASKS_ERRORS_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TASKS_FAILED_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TASKS_OK_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TASKS_TOTAL_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TIME_ELAPSED_KEY;
import static java.util.Objects.nonNull;

@Service
public class ValidationService {
    public static final String HEADER_KEY_VALUE_DELIMITER = "|";
    private final Duration TIMEOUT_SECONDS = Duration.ofSeconds(10);
    private Instant lrStart;
    private Instant lrEnd;
    private String lrStartDateTime;
    private int[] lrTaskCounts;
    private HttpClient client;
    private static Logger logger = LoggerFactory.getLogger(ValidationService.class);
    @Autowired
    private EmailNotificationService notificationService;
    @Autowired
    private XMLValidationTaskDao taskReader;
    @Autowired
    private Environment env;

    public ValidationService() {
        this.client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT_SECONDS)
            .followRedirects(HttpClient.Redirect.ALWAYS).build();
    }

    @Scheduled(cron = "${" + RUN_SCHEDULE_PROPERTY + "}")
    public void execValidations() throws FileNotFoundException, XMLParseException, ConnectIOException {
        Instant start = Instant.now();
        String startDT = EventListenerService.getCurrentDateTime();
        int[] taskCounts = new int[4];
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
            logger.error("HTTPClient's request for the validation task could not be completed", e);
            taskCounts[3]++;
        }

        List<String[]> failures = new ArrayList<>();
        for (int i = 0; i < resps.size(); i++) {
            ValidationTask task = tasks.get(i);
            HttpResponse<String> res = resps.get(i);
            String logmsg = "VALIDATION ";
            if (!task.isValid(res.statusCode(), res.body())) {
                String[] notifData = {task.reqURL(), String.valueOf(res.statusCode()), res.body()};
                failures.add(notifData);
                logmsg += "FAILURE";
            } else {
                logmsg += "OK";
                taskCounts[1]++;
            }
            logger.info(logmsg + " for: {}", task.reqURL());
        }
        if (failures.size() > 0) {
            this.notificationService.sendVTaskErrorsNotification(failures);
        }
        taskCounts[0] = tasks.size();
        taskCounts[2] = failures.size();
        this.lrTaskCounts = taskCounts;
        this.lrStartDateTime = startDT;
        this.lrStart = start;
        this.lrEnd = Instant.now();
    }

    public Map<String, String> getLastRunInfo() {
        Map<String, String> res = new HashMap<>();
        if (nonNull(this.lrEnd)) {
            res.put(START_TIME_KEY, this.lrStartDateTime);
            res.put(TIME_ELAPSED_KEY, String.valueOf(Duration.between(this.lrStart, this.lrEnd).getSeconds()));
            res.put(TASKS_TOTAL_KEY, String.valueOf(this.lrTaskCounts[0]));
            res.put(TASKS_OK_KEY, String.valueOf(this.lrTaskCounts[1]));
            res.put(TASKS_FAILED_KEY, String.valueOf(this.lrTaskCounts[2]));
            res.put(TASKS_ERRORS_KEY, String.valueOf(this.lrTaskCounts[3]));
        }

        return res;
    }

    public boolean isValidConfig() {
        return this.isValidCronExpression(this.env.getProperty(RUN_SCHEDULE_PROPERTY));
    }

    public boolean isValidCronExpression(String cronExpr) {
        boolean ans = true;
        if (!"-".equals(cronExpr)) {
            try {
                CronExpression.parse(cronExpr);
            } catch (IllegalArgumentException e) {
                ans = false;
            }
        }

        return ans;
    }

    void setClient(HttpClient client) { this.client = client; }

    void setNotificationService(EmailNotificationService service) { this.notificationService = service; }

    void setTaskReader(XMLValidationTaskDao taskReader) { this.taskReader = taskReader; }

    void setLogger(Logger logger) { ValidationService.logger = logger; }

    void setEnv(Environment env) { this.env = env; }
}
