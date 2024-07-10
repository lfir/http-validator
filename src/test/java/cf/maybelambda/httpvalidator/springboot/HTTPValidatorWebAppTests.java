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
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

public class HTTPValidatorWebAppTests {
    private final ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);

    @Test
    public void applicationContextLoadsWithSchedulingSupport() {
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            spring.when(() -> SpringApplication.run(HTTPValidatorWebApp.class, new String[] {})).thenReturn(this.context);
            Annotation annotation = HTTPValidatorWebApp.class.getAnnotation(EnableScheduling.class);

            HTTPValidatorWebApp.main(new String[] {});

            assertThat(annotation).isNotNull();
            spring.verify(() -> SpringApplication.run(HTTPValidatorWebApp.class, new String[] {}));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"--server.port=9000", "--cron.expression=0 0/1 * 1/1 * ?"})
    public void restartAppContextWithNewRunSchedulePreservesPreviousCLIArgs(String arg) throws InterruptedException {
        ApplicationArguments args = mock(ApplicationArguments.class);
        given(args.getSourceArgs()).willReturn(new String[]{arg});

        given(this.context.getBean(ApplicationArguments.class)).willReturn(args);
        HTTPValidatorWebApp.setContext(this.context);

        CountDownLatch latch = new CountDownLatch(1);
        // Mock the context.close method to count down the latch
        doAnswer(invocation -> {
            latch.countDown();
            return this.context;
        }).when(this.context).close();

        HTTPValidatorWebApp.restartAppContextWithNewRunSchedule("@daily");

        // Wait for the latch to count down (indicating the new thread started by restartApp... has finished)
        latch.await();

        verify(args).getSourceArgs();
        verify(this.context).getBean(ApplicationArguments.class);
        verify(this.context).close();
    }
}
