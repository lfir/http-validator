package cf.maybelambda.httpvalidator.springboot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.rmi.ConnectIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EventListenerServiceTests {
    private final EmailNotificationService mailServ = mock(EmailNotificationService.class);
    private EventListenerService eventServ;

    @BeforeEach
    void setUp() {
        this.eventServ = new EventListenerService();
        this.eventServ.setNotificationService(this.mailServ);
    }

    @Test
    void afterAppStartsStartTimeVariableIsNotNull() {
        this.eventServ.setAppStartTime();

        assertThat(this.eventServ.getStartTime()).isNotNull();
    }

    @Test
    void notifyThatAppTerminatedSendsEmailNotificationViaService() throws ConnectIOException {
        this.eventServ.notifyThatAppTerminated();

        verify(this.mailServ).sendAppTerminatedNotification(anyString());
    }
}
