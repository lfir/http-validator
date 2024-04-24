package cf.maybelambda.httpvalidator.springboot.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class EmailNotificationService {
    static final String BODY_LINE1 = "Request URL: ";
    static final String BODY_LINE2 = "Response Status Code: ";
    static final String BODY_LINE3 = "Response body: ";
    private SendGrid client;
    @Value("${notifications.from}")
    private String FROM;
    @Value("${notifications.to}")
    private String TO;
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);

    public EmailNotificationService(@Value("${sendgrid.apikey}") String apikey) {
        this.client = new SendGrid(apikey);
    }

    String buildMailBody(List<String[]> contents) {
        String res = "";
        for (String[] c : contents) {
            String p0 = BODY_LINE1 + c[0] + "\n";
            String p1 = BODY_LINE2 + c[1] + "\n";
            String p2 = BODY_LINE3 + (c[2] == null ? "" : StringUtils.truncate(c[2], 800, "..."));
            res += p0 + p1 + p2 + "\n\n\n";
        }

        return res;
    }

    public void sendVTaskErrorsNotification(List<String[]> mailBody) {
        Email from = new Email(FROM);
        from.setName("Chronos Maybelambda");
        String subject = "Notification of HTTP validation results";
        Email to = new Email(TO);
        Content content = new Content("text/plain", this.buildMailBody(mailBody));
        Mail mail = new Mail(from, subject, to, content);

        Request request = new Request();
        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        try {
            request.setBody(mail.build());
            Response res = this.client.api(request);
            logger.info("Email delivery result: Status Code: {}", res.getStatusCode() + " - Body: " + res.getBody());
        } catch (IOException e) {
            logger.error("Notification email delivery POST request could not be completed: {}", request);
            throw new RuntimeException(e);
        }
    }

    void setClient(SendGrid sg) { this.client = sg; }
}
