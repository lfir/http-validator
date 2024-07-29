package cf.maybelambda.httpvalidator.springboot.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.rmi.ConnectIOException;
import java.util.List;

import static cf.maybelambda.httpvalidator.springboot.util.HttpSendOutcomeWrapper.NET_ERR_CODE;
import static io.micrometer.common.util.StringUtils.isBlank;
import static io.micrometer.common.util.StringUtils.truncate;
import static java.util.Objects.isNull;

/**
 * Service to send email notifications using SendGrid.
 */
@Service
public class EmailNotificationService {
    static final String BODY_LINE1 = "Request URL: ";
    static final String BODY_LINE2 = "Response Status Code: ";
    static final String BODY_LINE3 = "Response body: ";
    static final String APIKEY_PROPERTY = "sendgrid.apikey";
    static final String FROM_PROPERTY = "notifications.from";
    static final String TO_PROPERTY = "notifications.to";
    private SendGrid client;
    private static Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    @Autowired
    private Environment env;

    /**
     * Builds the email body content from a list of validation results.
     *
     * @param contents A list of string arrays containing validation results.
     * @return The email body as a single string.
     */
    String buildMailBody(List<String[]> contents) {
        String res = "";
        for (String[] c : contents) {
            String p0 = BODY_LINE1 + c[0] + "\n";
            if (String.valueOf(NET_ERR_CODE).equals(c[1])) {
                res += p0 + c[2];
            } else {
                String p1 = BODY_LINE2 + c[1] + "\n";
                String p2 = BODY_LINE3 + (c[2] == null ? "" : truncate(c[2], 800, "..."));
                res += p0 + p1 + p2;
            }
            res += "\n\n\n";
        }

        return res;
    }

    /**
     * Sends a plain text email with the specified subject and body.
     *
     * @param subject The subject of the email.
     * @param body The body content of the email.
     * @throws ConnectIOException If an error occurs while sending the email.
     */
    void sendPlainTextEmail(String subject, String body) throws ConnectIOException {
        Email from = new Email(this.getFrom());
        from.setName("Chronos Maybelambda");
        Email to = new Email(this.getTo());
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, to, content);

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        try {
            request.setBody(mail.build());
            if (isNull(this.client)) { this.client = new SendGrid(this.getApiKey()); }
            Response res = this.client.api(request);
            logger.info("Email delivery result: Status Code: {}", res.getStatusCode() + " - Body: " + res.getBody());
        } catch (IOException e) {
            String errmsg = "POST request for delivery of the Notification Email could not be completed.";
            logger.error(errmsg);
            throw new ConnectIOException(errmsg, e);
        }
    }

    /**
     * Sends a notification email with validation task failure results.
     *
     * @param mailBody A list of validation task failure results.
     * @throws ConnectIOException If an error occurs while sending the email.
     */
    public void sendVTaskErrorsNotification(@NonNull List<String[]> mailBody) throws ConnectIOException {
        String subject = "Notification of HTTP validation results";
        String body = this.buildMailBody(mailBody);

        this.sendPlainTextEmail(subject, body);
    }

    /**
     * Sends a notification email about application termination.
     *
     * @param endTime The end time of the application.
     * @throws ConnectIOException If an error occurs while sending the email.
     */
    public void sendAppTerminatedNotification(String endTime) throws ConnectIOException {
        String subject = "HTTP Validator app terminated";
        String body = String.format("ContextClosedEvent occurred on: %s.", endTime);

        this.sendPlainTextEmail(subject, body);
    }

    /**
     * Checks if the email configuration is valid.
     * <p>
     * This method verifies that the necessary properties for sending emails
     * are properly configured in the environment. Specifically, it checks:
     * <ul>
     *     <li>The "notifications.from" property, which should contain the sender's email address.</li>
     *     <li>The "notifications.to" property, which should contain the recipient's email address.</li>
     *     <li>The "sendgrid.apikey" property, which should contain the SendGrid API key for authentication.</li>
     * </ul>
     * If any of these properties are blank or missing, the configuration is considered invalid.
     *
     * @return True if all necessary properties are set and non-blank, false otherwise.
     */
    public boolean isValidConfig() {
        return !(isBlank(this.getFrom()) || isBlank(this.getTo()) || isBlank(this.getApiKey()));
    }

    /**
     * Gets the SendGrid API key from the environment.
     *
     * @return The API key as a string.
     */
    private String getApiKey() { return this.env.getProperty(APIKEY_PROPERTY); }

    /**
     * Gets the email address through which notifications are sent.
     *
     * @return The sender email address.
     */
    String getFrom() { return this.env.getProperty(FROM_PROPERTY); }

    /**
     * Gets the email address to which notifications are sent.
     *
     * @return The recipient email address.
     */
    String getTo() { return this.env.getProperty(TO_PROPERTY); }

    /**
     * Sets the SendGrid client; for testing purposes.
     *
     * @param sg The SendGrid client to set.
     */
    void setClient(SendGrid sg) { this.client = sg; }

    /**
     * Sets the logger; for testing purposes.
     *
     * @param logger The logger to set.
     */
    void setLogger(Logger logger) { EmailNotificationService.logger = logger; }

    /**
     * Sets the environment; for testing purposes.
     *
     * @param env The environment to set.
     */
    void setEnv(Environment env) { this.env = env; }
}
