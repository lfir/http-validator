package cf.maybelambda.httpvalidator.springboot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.rmi.ConnectIOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service to handle application lifecycle events and send notifications.
 */
@Service
public class EventListenerService {
    private String startDateTime;

    @Autowired
    private EmailNotificationService mailServ;

    /**
     * Gets the current date and time formatted as per RFC 1123.
     *
     * @return The current date and time as a formatted string.
     */
    public static String getCurrentDateTime() {
        return OffsetDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    /**
     * Sets the application start time when the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void setAppStartTime() {
        this.startDateTime = getCurrentDateTime();
    }

    /**
     * Sends a notification when the application is terminated.
     *
     * @throws ConnectIOException if there is an issue sending the notification.
     */
    @EventListener(ContextClosedEvent.class)
    public void notifyThatAppTerminated() throws ConnectIOException {
        this.mailServ.sendAppTerminatedNotification(getCurrentDateTime());
    }

    /**
     * Gets the start date and time of the application.
     *
     * @return The start date and time as a string.
     */
    public String getStartDateTime() {
        return this.startDateTime;
    }

    /**
     * Sets the email notification service. Used for testing purposes.
     *
     * @param service The email notification service to set.
     */
    void setNotificationService(EmailNotificationService service) {
        this.mailServ = service;
    }
}
