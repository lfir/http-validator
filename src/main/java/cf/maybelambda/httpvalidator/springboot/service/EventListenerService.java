package cf.maybelambda.httpvalidator.springboot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.rmi.ConnectIOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EventListenerService {
    private String startDateTime;
    @Autowired
    private EmailNotificationService mailServ;

    public static String getCurrentDateTime() { return OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME); }

    @EventListener(ApplicationReadyEvent.class)
    public void setAppStartTime() {
        this.startDateTime = getCurrentDateTime();
    }

    @EventListener(ContextClosedEvent.class)
    public void notifyThatAppTerminated() throws ConnectIOException {
        this.mailServ.sendAppTerminatedNotification(getCurrentDateTime());
    }

    public String getStartDateTime() { return this.startDateTime; }

    void setNotificationService(EmailNotificationService service) { this.mailServ = service; }
}
