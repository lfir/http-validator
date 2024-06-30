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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.rmi.ConnectIOException;
import java.util.List;

import static io.micrometer.common.util.StringUtils.isBlank;
import static io.micrometer.common.util.StringUtils.truncate;

@Service
public class EmailNotificationService {
    static final String BODY_LINE1 = "Request URL: ";
    static final String BODY_LINE2 = "Response Status Code: ";
    static final String BODY_LINE3 = "Response body: ";
    @Value("${notifications.from}")
    private String from;
    @Value("${notifications.to}")
    private String to;
    private SendGrid client;
    private static Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);
    @Autowired
    private Environment env;

    public EmailNotificationService(@Value("${sendgrid.apikey}") String apikey) {
        this.client = new SendGrid(apikey);
    }

    String buildMailBody(List<String[]> contents) {
        String res = "";
        for (String[] c : contents) {
            String p0 = BODY_LINE1 + c[0] + "\n";
            String p1 = BODY_LINE2 + c[1] + "\n";
            String p2 = BODY_LINE3 + (c[2] == null ? "" : truncate(c[2], 800, "..."));
            res += p0 + p1 + p2 + "\n\n\n";
        }

        return res;
    }

    void sendPlainTextEmail(String subject, String body) throws ConnectIOException {
        Email from = new Email(this.from);
        from.setName("Chronos Maybelambda");
        Email to = new Email(this.to);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, to, content);

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        try {
            request.setBody(mail.build());
            Response res = this.client.api(request);
            logger.info("Email delivery result: Status Code: {}", res.getStatusCode() + " - Body: " + res.getBody());
        } catch (IOException e) {
            String errmsg = "POST request for delivery of the Notification Email could not be completed.";
            logger.error(errmsg);
            throw new ConnectIOException(errmsg, e);
        }
    }

    public void sendVTaskErrorsNotification(@NonNull List<String[]> mailBody) throws ConnectIOException {
        String subject = "Notification of HTTP validation results";
        String body = this.buildMailBody(mailBody);

        this.sendPlainTextEmail(subject, body);
    }

    public void sendAppTerminatedNotification(String endTime) throws ConnectIOException {
        String subject = "HTTP Validator app terminated";
        String body = String.format("ContextClosedEvent occurred on: %s.", endTime);

        this.sendPlainTextEmail(subject, body);
    }

    public boolean isValidConfig() {
        return !(isBlank(this.from) || isBlank(this.to) || isBlank(this.env.getProperty("sendgrid.apikey")));
    }

    void setClient(SendGrid sg) { this.client = sg; }

    void setLogger(Logger logger) { EmailNotificationService.logger = logger; }

    void setEnv(Environment env) { this.env = env; }

    void setFrom(String from) { this.from = from; }

    void setTo(String to) { this.to = to; }
}
