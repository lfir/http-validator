package cf.maybelambda.httpvalidator.springboot.service;

import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static cf.maybelambda.httpvalidator.springboot.service.EmailNotificationService.BODY_LINE1;
import static cf.maybelambda.httpvalidator.springboot.service.EmailNotificationService.BODY_LINE2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
public class EmailNotificationServiceTests {
    @Autowired
    EmailNotificationService service;

    @Test
    void buildMailBodyPreservesReceivedURLsAndStatusCodesInOutput() {
        String[] ss0 = { "http://localhost", "200", "tst", "https://site.com", "400", "" };
        String[] ss1 = { "https://site.com", "400", "" };
        List<String[]> res = new ArrayList<>();
        res.add(ss0);
        res.add(ss1);

        String ans = this.service.buildMailBody(res);

        assert ans.contains(BODY_LINE1 + ss0[0]);
        assert ans.contains(BODY_LINE2 + ss0[1]);
        assert ans.contains(BODY_LINE1 + ss1[0]);
        assert ans.contains(BODY_LINE2 + ss1[1]);
    }

    @Test
    void buildMailBodyTruncatesReceivedResponseBodyLongerThan800Characters() {
        String[] ss = { "", "", "" };
        for (int i = 0; i < 1500; i++) {
            ss[2] += "$";
        }
        List<String[]> res = new ArrayList<>();
        res.add(ss);

        String ans = this.service.buildMailBody(res);

        assertEquals(1500, ss[2].length());
        assert ans.length() < 900;
    }

    @Test
    void notificationServiceSendsEmailViaSendgridClient() throws IOException {
        SendGrid sg = mock(SendGrid.class);
        this.service.setClient(sg);
        String[] ss = { "", "", "" };
        List<String[]> res = new ArrayList<>();
        res.add(ss);

        this.service.sendNotification(res);

        verify(sg).api(any(Request.class));
    }
}
