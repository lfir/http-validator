package cf.maybelambda.httpvalidator.springboot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.lang.annotation.Annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

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

    @ParameterizedTest
    @ValueSource(strings = {"--server.port=9000", "--cron.expression=0 0/1 * 1/1 * ?"})
    public void restartAppContextWithNewRunSchedulePreservesPreviousCLIArgs(String s) {
        // TODO: make restartAppContextWithNewRunSchedule's test more comprehensive
        // spring.when(() -> SpringApplication.run... doesn't seem to work with:
        // context = SpringApplication.run(HTTPValidatorWebApp.class, args.toArray(String[]::new));
        // i.e. a new ApplicationContext is loaded anyway
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        ApplicationArguments args = mock(ApplicationArguments.class);
        given(context.getBean(ApplicationArguments.class)).willReturn(args);
        given(args.getSourceArgs()).willReturn(new String[]{s});
        HTTPValidatorWebApp.setContext(context);

        HTTPValidatorWebApp.restartAppContextWithNewRunSchedule("@daily");

        verify(context).getBean(ApplicationArguments.class);
    }
}
