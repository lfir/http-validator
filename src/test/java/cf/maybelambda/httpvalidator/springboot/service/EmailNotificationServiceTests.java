package cf.maybelambda.httpvalidator.springboot.service;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.rmi.ConnectIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cf.maybelambda.httpvalidator.springboot.service.EmailNotificationService.BODY_LINE1;
import static cf.maybelambda.httpvalidator.springboot.service.EmailNotificationService.BODY_LINE2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
public class EmailNotificationServiceTests {
    private final SendGrid sg = mock(SendGrid.class);
    @Autowired
    EmailNotificationService service;

    @Test
    void buildMailBodyPreservesReceivedURLsAndStatusCodesInOutput() {
        String[] ss0 = { "http://localhost", "200", "tst", "https://site.com", "400", "" };
        String[] ss1 = { "https://site.com", "400", null };
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
        Response res = mock(Response.class);
        given(res.getStatusCode()).willReturn(200);
        given(res.getBody()).willReturn("");
        given(this.sg.api(any(Request.class))).willReturn(res);
        this.service.setClient(this.sg);

        String[] ss = { "", "", "" };
        List<String[]> strs = new ArrayList<>();
        strs.add(ss);

        this.service.sendVTaskErrorsNotification(strs);

        assertThat(this.service.buildMailBody(strs)).isNotNull();
        verify(sg).api(any(Request.class));
    }

    @Test
    void whenSendVTaskErrorsNotificationFailsToSendEmailErrorIsLogged() throws IOException {
        given(this.sg.api(any(Request.class))).willThrow(IOException.class);
        this.service.setClient(this.sg);

        Logger logger = mock(Logger.class);
        this.service.setLogger(logger);

        assertThrows(ConnectIOException.class, () -> this.service.sendVTaskErrorsNotification(Collections.emptyList()));
        verify(logger).error(anyString());
    }
}
