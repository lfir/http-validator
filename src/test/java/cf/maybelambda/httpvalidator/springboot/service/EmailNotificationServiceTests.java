package cf.maybelambda.httpvalidator.springboot.service;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.rmi.ConnectIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cf.maybelambda.httpvalidator.springboot.service.EmailNotificationService.APIKEY_PROPERTY;
import static cf.maybelambda.httpvalidator.springboot.service.EmailNotificationService.BODY_LINE1;
import static cf.maybelambda.httpvalidator.springboot.service.EmailNotificationService.BODY_LINE2;
import static cf.maybelambda.httpvalidator.springboot.service.EmailNotificationService.FROM_PROPERTY;
import static cf.maybelambda.httpvalidator.springboot.service.EmailNotificationService.TO_PROPERTY;
import static cf.maybelambda.httpvalidator.springboot.util.HttpSendOutcomeWrapper.NET_ERR_CODE;
import static cf.maybelambda.httpvalidator.springboot.util.HttpSendOutcomeWrapper.NET_ERR_MSG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EmailNotificationServiceTests {
    private final Logger logger = mock(Logger.class);
    private final SendGrid sg = mock(SendGrid.class);
    private final Environment env = mock(Environment.class);
    private EmailNotificationService mailServ;

    @BeforeEach
    void setUp() {
        this.mailServ = new EmailNotificationService();
        this.mailServ.setClient(this.sg);
        this.mailServ.setEnv(this.env);
    }

    @Test
    void buildMailBodyPreservesReceivedURLsAndStatusCodesInOutput() {
        String[] ss0 = { "http://localhost", "200", "tst", "https://site.com", "400", "" };
        String[] ss1 = { "https://site.com", "400", null };
        List<String[]> res = new ArrayList<>();
        res.add(ss0);
        res.add(ss1);

        String ans = this.mailServ.buildMailBody(res);

        assertThat(ans.contains(BODY_LINE1 + ss0[0])).isTrue();
        assertThat(ans.contains(BODY_LINE2 + ss0[1])).isTrue();
        assertThat(ans.contains(BODY_LINE1 + ss1[0])).isTrue();
        assertThat(ans.contains(BODY_LINE2 + ss1[1])).isTrue();
    }

    @Test
    void buildMailBodyTruncatesReceivedResponseBodyLongerThan800Characters() {
        String[] ss = { "", "", "" };
        for (int i = 0; i < 1500; i++) {
            ss[2] += "$";
        }
        List<String[]> res = new ArrayList<>();
        res.add(ss);

        String ans = this.mailServ.buildMailBody(res);

        assertEquals(1500, ss[2].length());
        assertThat(ans.length()).isLessThan(900);
    }

    @Test
    void buildMailBodyIncludesNetworkErrorMessageWhenNetworkErrorStatusCodeIsReceived() {
        String[] ss0 = { "http://localhost", String.valueOf(NET_ERR_CODE), NET_ERR_MSG };
        List<String[]> res = new ArrayList<>();
        res.add(ss0);

        String ans = this.mailServ.buildMailBody(res);

        assertThat(ans.contains(NET_ERR_MSG)).isTrue();
    }

    @Test
    void sendVTaskErrorsNotificationSendsEmailViaSendgridClient() throws Exception {
        Response res = mock(Response.class);
        given(res.getStatusCode()).willReturn(200);
        given(res.getBody()).willReturn("");
        given(this.sg.api(any(Request.class))).willReturn(res);

        String[] ss = { "", "", "" };
        List<String[]> strs = new ArrayList<>();
        strs.add(ss);

        this.mailServ.sendVTaskErrorsNotification(strs);

        assertThat(this.mailServ.buildMailBody(strs)).isNotNull();
        verify(this.sg).api(any(Request.class));
    }

    @Test
    void sendAppTerminatedNotificationSendsEmailViaSendgridClient() throws Exception {
        Response res = mock(Response.class);
        given(res.getStatusCode()).willReturn(200);
        given(res.getBody()).willReturn("");
        given(this.sg.api(any(Request.class))).willReturn(res);

        this.mailServ.sendAppTerminatedNotification("");

        verify(this.sg).api(any(Request.class));
    }

    @Test
    void whenSendVTaskErrorsNotificationFailsToSendEmailErrorIsLogged() throws Exception {
        given(this.sg.api(any(Request.class))).willThrow(IOException.class);

        this.mailServ.setLogger(this.logger);

        assertThrows(ConnectIOException.class, () -> this.mailServ.sendVTaskErrorsNotification(Collections.emptyList()));
        verify(this.logger).error(anyString());
    }

    @Test
    void whenFromToAndApiKeyAreNullIsValidConfigReturnsFalse() {
        given(this.env.getProperty(anyString())).willReturn(null);

        assertThat(this.mailServ.isValidConfig()).isFalse();
    }

    @Test
    void whenFromToOrApiKeyAreEmptyIsValidConfigReturnsFalse() {
        given(this.env.getProperty(APIKEY_PROPERTY)).willReturn("apiKey");
        given(this.env.getProperty(FROM_PROPERTY)).willReturn("");
        given(this.env.getProperty(TO_PROPERTY)).willReturn("b@b.com");
        assertThat(this.mailServ.isValidConfig()).isFalse();

        given(this.env.getProperty(APIKEY_PROPERTY)).willReturn("apiKey");
        given(this.env.getProperty(FROM_PROPERTY)).willReturn("a@a.com");
        given(this.env.getProperty(TO_PROPERTY)).willReturn("");
        assertThat(this.mailServ.isValidConfig()).isFalse();

        given(this.env.getProperty(APIKEY_PROPERTY)).willReturn("");
        given(this.env.getProperty(FROM_PROPERTY)).willReturn("a@a.com");
        given(this.env.getProperty(TO_PROPERTY)).willReturn("b@b.com");
        assertThat(this.mailServ.isValidConfig()).isFalse();
    }

    @Test
    void whenFromToAndApiKeyAreNotEmptyOrNullIsValidConfigReturnsTrue() {
        given(this.env.getProperty(APIKEY_PROPERTY)).willReturn("apiKey");
        given(this.env.getProperty(FROM_PROPERTY)).willReturn("a@a.com");
        given(this.env.getProperty(TO_PROPERTY)).willReturn("b@b.com");

        assertThat(this.mailServ.isValidConfig()).isTrue();
    }

    @Test
    void emailNotificationServiceInstanceUsesNewSendGridClientToSendEmailWhenNoneWasSetBefore() throws Exception {
        EmailNotificationService serv = new EmailNotificationService();
        serv.setEnv(this.env);
        serv.setLogger(this.logger);

        serv.sendAppTerminatedNotification("");

        verify(this.logger).info(anyString(), anyString());
    }
}
