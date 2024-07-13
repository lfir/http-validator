package cf.maybelambda.httpvalidator.springboot;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main class for starting the HTTP Validator web application.
 * Finds components, reads configuration, and starts the web server.
 */
@SpringBootApplication
@EnableScheduling
public class HTTPValidatorWebApp {
    public static final String RUN_SCHEDULE_PROPERTY = "cron.expression";
    private static ConfigurableApplicationContext context;

    /**
     * Main method to run the Spring Boot application.
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        context = SpringApplication.run(HTTPValidatorWebApp.class, args);
    }

    /**
     * Restarts the application context with a new cron expression for scheduling tasks.
     * This method creates a new thread to close the current context and restart the application
     * with updated arguments that override the expression previously used.
     * @param cronExpression New cron expression
     */
    public static void restartAppContextWithNewRunSchedule(String cronExpression) {
        String[] origArgs = context.getBean(ApplicationArguments.class).getSourceArgs();
        List<String> args = new ArrayList<>(Arrays.stream(origArgs).filter(a -> !a.contains(RUN_SCHEDULE_PROPERTY)).toList());
        args.add("--" + RUN_SCHEDULE_PROPERTY + "=" + cronExpression);

        Thread thread = new Thread(() -> {
            context.close();
            context = SpringApplication.run(HTTPValidatorWebApp.class, args.toArray(String[]::new));
        });

        thread.setDaemon(false);
        thread.start();
    }

    /**
     * Sets the application context. Used for testing purposes.
     * @param context Application context
     */
    static void setContext(ConfigurableApplicationContext context) {
        HTTPValidatorWebApp.context = context;
    }
}
