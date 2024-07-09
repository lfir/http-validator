package cf.maybelambda.httpvalidator.springboot;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

@ActiveProfiles("test")
public class HTTPValidatorWebAppTests {
    @Test
    public void applicationContextLoadsWithSchedulingSupport() {
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            spring.when(() -> SpringApplication.run(HTTPValidatorWebApp.class, new String[] {})).thenReturn(null);
            Annotation annotation = HTTPValidatorWebApp.class.getAnnotation(EnableScheduling.class);

            HTTPValidatorWebApp.main(new String[] {});

            assertThat(annotation).isNotNull();
            spring.verify(() -> SpringApplication.run(HTTPValidatorWebApp.class, new String[] {}));
        }
    }
}
