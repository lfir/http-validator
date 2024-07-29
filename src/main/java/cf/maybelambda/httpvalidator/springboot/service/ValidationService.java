package cf.maybelambda.httpvalidator.springboot.service;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import cf.maybelambda.httpvalidator.springboot.util.HttpSendOutcomeWrapper;
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
import java.util.stream.IntStream;

import static cf.maybelambda.httpvalidator.springboot.HTTPValidatorWebApp.RUN_SCHEDULE_PROPERTY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.START_TIME_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TASKS_FAILED_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TASKS_OK_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TASKS_TOTAL_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TIME_ELAPSED_KEY;
import static java.util.Objects.nonNull;

/**
 * Service for handling validation tasks, executing scheduled validations,
 * and managing validation results.
 */
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

    /**
     * Constructor to initialize the HTTP client with default connection-timeout and follow-redirects settings.
     */
    public ValidationService() {
        this.client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT_SECONDS)
            .followRedirects(HttpClient.Redirect.ALWAYS).build();
    }

    /**
     * Executes validation tasks based on a cron schedule.
     * Retrieves tasks, sends HTTP requests, and processes responses.
     * Sends email notifications for any validation failures.
     * Updates last run information with results of validation tasks.
     *
     * @throws FileNotFoundException if the validation tasks file is not found
     * @throws XMLParseException if there is an error parsing the XML file
     * @throws ConnectIOException if there is an error sending notification email
     * @throws ExecutionException when an unhandled error occurs while processing the HTTP requests
     * @throws InterruptedException when interrupted before completing all the requests
     */
    @Scheduled(cron = "${" + RUN_SCHEDULE_PROPERTY + "}")
    public void execValidations() throws FileNotFoundException, XMLParseException, ConnectIOException, ExecutionException, InterruptedException {
        Instant start = Instant.now();
        String startDT = EventListenerService.getCurrentDateTime();
        int[] taskCounts = new int[3];
        List<HttpRequest> reqs = new ArrayList<>();
        List<ValidationTask> tasks = this.taskReader.getAll();
        for (ValidationTask task : tasks) {
            HttpRequest.Builder req = HttpRequest.newBuilder();
            req.uri(URI.create(task.reqURL()));
            task.reqHeaders().forEach(h -> req.headers(h.split(Pattern.quote(HEADER_KEY_VALUE_DELIMITER))));
            req.timeout(TIMEOUT_SECONDS);
            reqs.add(req.build());
        }

        List<HttpSendOutcomeWrapper> results = new ArrayList<>(reqs.size());
        IntStream.range(0, reqs.size()).forEach(i -> results.add(null));
        // Use the index of each request to store the corresponding response or exception
        List<CompletableFuture<Void>> futures = IntStream.range(0, reqs.size())
            .mapToObj(i -> client.sendAsync(reqs.get(i), HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> results.set(i, new HttpSendOutcomeWrapper(res)))
                .exceptionally(e -> {
                    results.set(i, new HttpSendOutcomeWrapper(e));
                    return null;
                }))
            .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        List<String[]> failures = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            ValidationTask task = tasks.get(i);
            HttpSendOutcomeWrapper res = results.get(i);
            String logmsg = "VALIDATION ";
            if (res.isWholeResponse() && task.isValid(res.getStatusCode(), res.getBody())) {
                logmsg += "OK";
                taskCounts[1]++;
            } else {
                failures.add(new String[]{task.reqURL(), String.valueOf(res.getStatusCode()), res.getBody()});
                logmsg += "FAILURE";
                taskCounts[2]++;
            }
            logger.info(logmsg + " for: " + task.reqURL());
        }
        if (!failures.isEmpty()) {
            this.notificationService.sendVTaskErrorsNotification(failures);
        }
        taskCounts[0] = tasks.size();
        this.lrTaskCounts = taskCounts;
        this.lrStartDateTime = startDT;
        this.lrStart = start;
        this.lrEnd = Instant.now();
    }

    /**
     * Retrieves information about the last run of validation tasks.
     *
     * @return A map containing start time, time elapsed, total tasks, successful tasks and failed tasks.
     */
    public Map<String, String> getLastRunInfo() {
        Map<String, String> res = new HashMap<>();
        if (nonNull(this.lrEnd)) {
            res.put(START_TIME_KEY, this.lrStartDateTime);
            res.put(TIME_ELAPSED_KEY, String.valueOf(Duration.between(this.lrStart, this.lrEnd).getSeconds()));
            res.put(TASKS_TOTAL_KEY, String.valueOf(this.lrTaskCounts[0]));
            res.put(TASKS_OK_KEY, String.valueOf(this.lrTaskCounts[1]));
            res.put(TASKS_FAILED_KEY, String.valueOf(this.lrTaskCounts[2]));
        }

        return res;
    }

    /**
     * Checks if the configuration is valid by validating the cron expression currently in use.
     *
     * @return true if the configuration is valid, false otherwise
     */
    public boolean isValidConfig() {
        return this.isValidCronExpression(this.env.getProperty(RUN_SCHEDULE_PROPERTY));
    }

    /**
     * Validates a given cron expression.
     *
     * @param cronExpr Cron expression to validate
     * @return true if the cron expression is valid, false otherwise
     */
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

    /**
     * Sets the HTTP client. Used for testing purposes.
     *
     * @param client HTTP client
     */
    void setClient(HttpClient client) { this.client = client; }

    /**
     * Sets the email notification service. Used for testing purposes.
     *
     * @param service Email notification service
     */
    void setNotificationService(EmailNotificationService service) { this.notificationService = service; }

    /**
     * Sets the validation task reader service. Used for testing purposes.
     *
     * @param taskReader XML validation task DAO
     */
    void setTaskReader(XMLValidationTaskDao taskReader) { this.taskReader = taskReader; }

    /**
     * Sets the logger. Used for testing purposes.
     *
     * @param logger Logger
     */
    void setLogger(Logger logger) { ValidationService.logger = logger; }

    /**
     * Sets the environment object. Used for testing purposes.
     *
     * @param env Environment
     */
    void setEnv(Environment env) { this.env = env; }
}
