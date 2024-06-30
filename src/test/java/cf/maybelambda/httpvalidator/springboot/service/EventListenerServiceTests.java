package cf.maybelambda.httpvalidator.springboot.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.rmi.ConnectIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
public class EventListenerServiceTests {
    @MockBean
    private EmailNotificationService mailServ;
    @Autowired
    private EventListenerService eventServ;

    @Test
    void afterAppStartsStartTimeVariableIsNotNull() {
        assertThat(this.eventServ.getStartTime()).isNotNull();
    }

    @Test
    void notifyThatAppTerminatedSendsEmailNotificationViaService() throws ConnectIOException {
        this.eventServ.notifyThatAppTerminated();

        verify(mailServ).sendAppTerminatedNotification(anyString());
    }
}
