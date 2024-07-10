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
 * Find components, read configuration and start the web server.
 */
@SpringBootApplication
@EnableScheduling
public class HTTPValidatorWebApp {
    public static final String RUN_SCHEDULE_PROPERTY = "cron.expression";
    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(HTTPValidatorWebApp.class, args);
    }

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

    static void setContext(ConfigurableApplicationContext context) { HTTPValidatorWebApp.context = context; }
}
