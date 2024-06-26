package cf.maybelambda.httpvalidator.springboot.service;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import javax.management.modelmbean.XMLParseException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.rmi.ConnectIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static cf.maybelambda.httpvalidator.springboot.service.ValidationService.HEADER_KEY_VALUE_DELIMITER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
public class ValidationServiceTests {
    private final HttpResponse<String> res = mock(HttpResponse.class);
    private final EmailNotificationService ns = mock(EmailNotificationService.class);
    private final HttpClient cl = mock(HttpClient.class);
    private final XMLValidationTaskDao dao = mock(XMLValidationTaskDao.class);
    private final Logger logger = mock(Logger.class);
    private final List<ValidationTask> tasks = new ArrayList<>();
    private final HttpRequest req = mock(HttpRequest.class);
    private final Environment env = mock(Environment.class);
    @Autowired
    private ValidationService vs;

    @BeforeEach
    void setUp() {
        this.vs.setClient(this.cl);
        this.vs.setNotificationService(this.ns);
        this.vs.setTaskReader(this.dao);
        this.vs.setLogger(this.logger);
        this.tasks.clear();
        this.vs.setEnv(env);

        given(this.req.uri()).willReturn(URI.create("http://localhost"));
        given(this.res.request()).willReturn(this.req);
        given(this.res.statusCode()).willReturn(200);
    }

    @Test
    void execValidationsSendsRequestAndNotificationViaHTTPAndEmailClients() throws ConnectIOException, XMLParseException {
        given(this.res.body()).willReturn("");
        given(this.cl.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .willReturn(CompletableFuture.completedFuture(this.res));

        tasks.add(new ValidationTask(0, "http://localhost", Collections.emptyList(), 0, ""));
        given(this.dao.getAll()).willReturn(this.tasks);

        this.vs.execValidations();

        assertEquals(1, this.dao.getAll().size());
        verify(this.cl).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(this.ns).sendVTaskErrorsNotification(anyList());
    }

    @Test
    void whenExceptionOccursDuringExecValidationsRequestErrorIsLogged() throws ConnectIOException, XMLParseException {
        given(this.cl.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .willReturn(CompletableFuture.failedFuture(new InterruptedException("Simulated Exception")));

        List<String> headers = new ArrayList<>();
        headers.add(String.format("X%s0", HEADER_KEY_VALUE_DELIMITER));
        this.tasks.add(new ValidationTask(0, "http://localhost", headers, 0, ""));
        given(this.dao.getAll()).willReturn(this.tasks);

        this.vs.execValidations();

        assertEquals(1, this.tasks.get(0).reqHeaders().size());
        verify(this.logger).error(anyString(), any(Exception.class));
    }

    @Test
    void execValidationsLogsValidTaskResult() throws ConnectIOException, XMLParseException {
        given(this.res.body()).willReturn("");
        given(this.cl.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .willReturn(CompletableFuture.completedFuture(this.res));

        this.tasks.add(new ValidationTask(0, "http://localhost", Collections.emptyList(), 200, ""));
        given(this.dao.getAll()).willReturn(this.tasks);

        this.vs.execValidations();

        assertThat(this.dao.getAll().get(0).reqHeaders().isEmpty()).isTrue();
        verify(this.logger).info(anyString(), anyString());
    }

    @Test
    void execValidationsLogsInvalidTaskResult() throws ConnectIOException, XMLParseException {
        given(this.res.body()).willReturn(null);
        given(this.cl.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .willReturn(CompletableFuture.completedFuture(this.res));

        this.tasks.add(new ValidationTask(0, "http://localhost", Collections.emptyList(), 200, ""));
        given(this.dao.getAll()).willReturn(this.tasks);

        this.vs.execValidations();

        assertThat(this.dao.getAll()).isNotEmpty();
        verify(this.logger).info(anyString(), anyString());
    }

    @Test
    void whenDashCronExpressionIsValidConfigReturnsTrue() {
        given(this.env.getProperty(anyString())).willReturn("-");

        assertThat(this.vs.isValidConfig()).isTrue();
    }

    @Test
    void whenDailyMacroCronExpressionIsValidConfigReturnsTrue() {
        given(this.env.getProperty(anyString())).willReturn("@daily");

        assertThat(this.vs.isValidConfig()).isTrue();
    }

    @Test
    void whenInvalidCronExpressionIsValidConfigReturnsFalse() {
        given(this.env.getProperty(anyString())).willReturn("*");

        assertThat(this.vs.isValidConfig()).isFalse();
    }

    @Test
    void whenNullCronExpressionIsValidConfigReturnsFalse() {
        given(this.env.getProperty(anyString())).willReturn(null);

        assertThat(this.vs.isValidConfig()).isFalse();
    }
}
