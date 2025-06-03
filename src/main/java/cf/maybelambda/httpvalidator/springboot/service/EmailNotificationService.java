package cf.maybelambda.httpvalidator.springboot.service;

import com.mailgun.api.v3.MailgunMessagesApi;
import com.mailgun.client.MailgunClient;
import com.mailgun.model.message.Message;
import com.mailgun.model.message.MessageResponse;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.rmi.ConnectIOException;
import java.util.List;

import static cf.maybelambda.httpvalidator.springboot.util.HttpSendOutcomeWrapper.NET_ERR_CODE;
import static io.micrometer.common.util.StringUtils.isBlank;
import static io.micrometer.common.util.StringUtils.truncate;

/**
 * Service to send email notifications using external SMTP service.
 */
@Service
public class EmailNotificationService {
    static final String BODY_LINE1 = "Request URL: ";
    static final String BODY_LINE2 = "Response Status Code: ";
    static final String BODY_LINE3 = "Response body: ";
    static final String APIKEY_PROPERTY = "mailer.apikey";
    static final String FROM_PROPERTY = "notifications.from";
    static final String TO_PROPERTY = "notifications.to";
    private MailgunMessagesApi client;
    private static Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    @Autowired
    private Environment env;

    public EmailNotificationService(@Value("${" + APIKEY_PROPERTY + "}") String apiKey) {
        this.client = MailgunClient.config(apiKey).createApi(MailgunMessagesApi.class);
    }

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
        if (!this.isValidConfig()) return;

        Message message = Message.builder()
            .from(this.getFrom())
            .to(this.getTo())
            .subject(subject)
            .text(body)
            .build();

        try {
            MessageResponse res = this.client.sendMessage(this.getFrom().split("@")[1], message);
            logger.info("Email delivery result: " + res.getMessage());
        } catch (FeignException e) {
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
     *     <li>The "mailer.apikey" property, which should contain the API key for authentication with SMTP service.</li>
     * </ul>
     * If any of these properties are blank or missing, the configuration is considered invalid.
     *
     * @return True if all necessary properties are set and non-blank, false otherwise.
     */
    public boolean isValidConfig() {
        return !(isBlank(this.getFrom()) || isBlank(this.getTo()) || isBlank(this.getApiKey()));
    }

    /**
     * Gets the API key of the mailing service from the environment.
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
     * Sets the Mailer client; for testing purposes.
     *
     * @param cl The client to set.
     */
    void setClient(MailgunMessagesApi cl) { this.client = cl; }

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
