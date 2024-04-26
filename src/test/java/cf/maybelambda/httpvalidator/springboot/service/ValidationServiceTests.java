package cf.maybelambda.httpvalidator.springboot.service;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
public class ValidationServiceTests {
    public static final HttpResponse<String> r = mock(HttpResponse.class);
    private final EmailNotificationService ns = mock(EmailNotificationService.class);
    private final HttpClient cl = mock(HttpClient.class);
    private final XMLValidationTaskDao dao = mock(XMLValidationTaskDao.class);
    private final Logger logger = mock(Logger.class);
    private final List<ValidationTask> tasks = new ArrayList<>();
    @Autowired
    private ValidationService vs;

    @BeforeEach
    void setUp() {
        this.vs.setClient(this.cl);
        this.vs.setNotificationService(this.ns);
        this.vs.setTaskReader(this.dao);
        this.vs.setLogger(this.logger);
        this.tasks.clear();
    }

    @Test
    void execValidationsSendsRequestAndNotificationViaHTTPAndEmailClients() throws IOException, InterruptedException {
        given(this.cl.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).willReturn(mock(HttpResponse.class));

        tasks.add(new ValidationTask(0, "http://localhost", Collections.emptyList(), 0, ""));
        given(this.dao.getAll()).willReturn(tasks);

        this.vs.execValidations();

        assertEquals(1, this.dao.getAll().size());
        verify(this.cl).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(this.ns).sendVTaskErrorsNotification(anyList());
    }

    @Test
    void whenExceptionOccursDuringExecValidationsRequestErrorIsLogged() throws IOException, InterruptedException {
        given(this.cl.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).willThrow(InterruptedException.class);

        List<String> headers = new ArrayList<>();
        headers.add("X:0");
        this.tasks.add(new ValidationTask(0, "http://localhost", headers, 0, ""));
        given(this.dao.getAll()).willReturn(tasks);

        this.vs.execValidations();

        assertEquals(1, this.tasks.get(0).reqHeaders().size());
        verify(this.logger).error(anyString(), any(Exception.class));
    }

    @Test
    void execValidationsLogsValidTaskResult() throws IOException, InterruptedException {
        HttpResponse<String> r = mock(HttpResponse.class);
        given(r.statusCode()).willReturn(200);
        given(r.body()).willReturn("");
        given(this.cl.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).willReturn(r);

        this.tasks.add(new ValidationTask(0, "http://localhost", Collections.emptyList(), 200, ""));
        given(this.dao.getAll()).willReturn(tasks);

        this.vs.execValidations();

        assert this.dao.getAll().get(0).reqHeaders().isEmpty();
        verify(this.logger).info(anyString(), anyString());
    }

    @Test
    void execValidationsLogsInvalidTaskResult() throws IOException, InterruptedException {
        given(r.statusCode()).willReturn(200);
        given(r.body()).willReturn("");
        given(this.cl.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).willReturn(r);

        this.tasks.add(new ValidationTask(0, "http://localhost", Collections.emptyList(), 200, "VAL"));
        given(this.dao.getAll()).willReturn(tasks);

        this.vs.execValidations();

        assertThat(this.dao.getAll()).isNotEmpty();
        verify(this.logger).info(anyString(), anyString());
    }
}
