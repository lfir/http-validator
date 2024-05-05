package cf.maybelambda.httpvalidator.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Find components, read configuration and start the web server.
 */
@SpringBootApplication
@EnableScheduling
public class HTTPValidatorWebApp {
    public static void main(String[] args) {
        SpringApplication.run(HTTPValidatorWebApp.class, args);
    }
}
