package cf.maybelambda.httpvalidator.springboot.service;

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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.rmi.ConnectIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class EmailNotificationServiceTests {
    private final Logger logger = mock(Logger.class);
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final Environment env = mock(Environment.class);
    private EmailNotificationService mailServ;

    @BeforeEach
    void setUp() {
        this.mailServ = new EmailNotificationService(this.mailSender);
        this.mailServ.setEnv(this.env);
        this.mailServ.setLogger(this.logger);
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
    void sendVTaskErrorsNotificationSendsEmailViaMailerClient() throws Exception {
        given(this.env.getProperty(FROM_PROPERTY)).willReturn("a@a.com");
        given(this.env.getProperty(TO_PROPERTY)).willReturn("b@b.com");

        String[] ss = { "", "", "" };
        List<String[]> strs = new ArrayList<>();
        strs.add(ss);

        this.mailServ.sendVTaskErrorsNotification(strs);

        assertThat(this.mailServ.buildMailBody(strs)).isNotNull();
        verify(this.mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendAppTerminatedNotificationSendsEmailViaMailerClient() throws Exception {
        given(this.env.getProperty(FROM_PROPERTY)).willReturn("a@a.com");
        given(this.env.getProperty(TO_PROPERTY)).willReturn("b@b.com");

        this.mailServ.sendAppTerminatedNotification("");

        verify(this.mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void whenSendVTaskErrorsNotificationFailsToSendEmailErrorIsLogged() {
        given(this.env.getProperty(FROM_PROPERTY)).willReturn("a@a.com");
        given(this.env.getProperty(TO_PROPERTY)).willReturn("b@b.com");
        doThrow(new MailSendException("Failed to send")).when(this.mailSender).send(any(SimpleMailMessage.class));

        assertThrows(ConnectIOException.class,
                () -> this.mailServ.sendVTaskErrorsNotification(Collections.emptyList()));
        verify(this.logger).error(anyString());
    }

    @Test
    void whenFromAndToAreNullIsValidConfigReturnsFalse() {
        given(this.env.getProperty(anyString())).willReturn(null);

        assertThat(this.mailServ.isValidConfig()).isFalse();
    }

    @Test
    void whenFromOrToAreEmptyIsValidConfigReturnsFalse() {
        given(this.env.getProperty(FROM_PROPERTY)).willReturn("");
        given(this.env.getProperty(TO_PROPERTY)).willReturn("b@b.com");
        assertThat(this.mailServ.isValidConfig()).isFalse();

        given(this.env.getProperty(FROM_PROPERTY)).willReturn("a@a.com");
        given(this.env.getProperty(TO_PROPERTY)).willReturn("");
        assertThat(this.mailServ.isValidConfig()).isFalse();
    }

    @Test
    void whenPropertiesAreValidAndMailSenderIsNotNullIsValidConfigReturnsTrue() {
        given(this.env.getProperty(FROM_PROPERTY)).willReturn("a@a.com");
        given(this.env.getProperty(TO_PROPERTY)).willReturn("b@b.com");

        assertThat(this.mailServ.isValidConfig()).isTrue();
    }

    @Test
    void whenMailSenderIsNullIsValidConfigReturnsFalse() {
        given(this.env.getProperty(FROM_PROPERTY)).willReturn("a@a.com");
        given(this.env.getProperty(TO_PROPERTY)).willReturn("b@b.com");
        EmailNotificationService serv = new EmailNotificationService(this.mailSender);
        serv.setEnv(this.env);

        assertThat(serv.isValidConfig()).isTrue();
    }

    @Test
    void sendPlainTextEmailDoesNotAttemptRequestWhenConfigurationIsInvalid() throws Exception {
        this.mailServ.sendAppTerminatedNotification("");

        verifyNoInteractions(this.mailSender);
    }
}
