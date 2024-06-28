package cf.maybelambda.httpvalidator.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Find components, read configuration and start the web server.
 */
@SpringBootApplication
@EnableScheduling
public class HTTPValidatorWebApp {
    public static String startTime;

    public static void main(String[] args) {
        SpringApplication.run(HTTPValidatorWebApp.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerStartTime() {
        startTime = OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}
