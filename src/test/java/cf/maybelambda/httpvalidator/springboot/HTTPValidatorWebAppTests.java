package cf.maybelambda.httpvalidator.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class HTTPValidatorWebAppTests {
    @Test
    public void applicationContextLoadsWithSchedulingSupport() {
        Annotation annotation = HTTPValidatorWebApp.class.getAnnotation(EnableScheduling.class);

        HTTPValidatorWebApp.main(new String[] {});

        assertThat(annotation).isNotNull();
    }
}
