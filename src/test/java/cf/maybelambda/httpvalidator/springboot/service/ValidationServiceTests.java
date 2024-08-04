package cf.maybelambda.httpvalidator.springboot.service;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.START_TIME_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TASKS_FAILED_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TASKS_OK_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TASKS_TOTAL_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TIME_ELAPSED_KEY;
import static cf.maybelambda.httpvalidator.springboot.service.ValidationService.HEADER_KEY_VALUE_DELIMITER;
import static java.util.Collections.emptyList;
import static javax.swing.text.html.FormSubmitEvent.MethodType.GET;
import static javax.swing.text.html.FormSubmitEvent.MethodType.POST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ValidationServiceTests {
    private final ObjectMapper mapper = mock(ObjectMapper.class);
    private final HttpResponse<String> res = mock(HttpResponse.class);
    private final EmailNotificationService ns = mock(EmailNotificationService.class);
    private final HttpClient cl = mock(HttpClient.class);
    private final XMLValidationTaskDao dao = mock(XMLValidationTaskDao.class);
    private final Logger logger = mock(Logger.class);
    private final List<ValidationTask> tasks = new ArrayList<>();
    private final HttpRequest req = mock(HttpRequest.class);
    private final Environment env = mock(Environment.class);
    private final JsonNode reqBody = mock(JsonNode.class);
    private ValidationService vs;

    @BeforeEach
    void setUp() {
        this.vs = new ValidationService();
        this.vs.setClient(this.cl);
        this.vs.setNotificationService(this.ns);
        this.vs.setTaskReader(this.dao);
        this.vs.setLogger(this.logger);
        this.tasks.clear();
        this.vs.setEnv(env);
        this.vs.setObjectMapper(this.mapper);

        given(this.req.uri()).willReturn(URI.create("http://localhost"));
        given(this.res.request()).willReturn(this.req);
        given(this.res.statusCode()).willReturn(200);
    }

    @Test
    void execValidationsSendsRequestAndNotificationViaHTTPAndEmailClients() throws Exception {
        given(this.res.body()).willReturn("");
        given(this.cl.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .willReturn(CompletableFuture.completedFuture(this.res));

        this.tasks.add(
            new ValidationTask(GET, "http://localhost", emptyList(), this.reqBody,0, "")
        );
        given(this.dao.getAll()).willReturn(this.tasks);

        this.vs.execValidations();

        assertEquals(1, this.dao.getAll().size());
        verify(this.cl).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(this.ns).sendVTaskErrorsNotification(anyList());
    }

    @Test
    void execValidationsSendsPOSTRequestWithBodyWhenTaskMethodIsPOST() throws Exception {
        given(this.cl.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .willReturn(CompletableFuture.completedFuture(this.res));
        given(this.mapper.writeValueAsString(any(JsonNode.class))).willReturn("");

        this.tasks.add(
            new ValidationTask(POST, "http://localhost", emptyList(), this.reqBody,200, "")
        );
        given(this.dao.getAll()).willReturn(this.tasks);

        this.vs.execValidations();

        verify(this.mapper).writeValueAsString(this.reqBody);
        verify(this.cl).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void whenExceptionOccursDuringExecValidationsRequestNotificationIsSent() throws Exception {
        given(this.cl.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .willReturn(CompletableFuture.failedFuture(new InterruptedException("Simulated Exception")));

        List<String> headers = new ArrayList<>();
        headers.add(String.format("X%s0", HEADER_KEY_VALUE_DELIMITER));
        this.tasks.add(
            new ValidationTask(GET, "http://localhost", headers, this.reqBody,0, "")
        );
        given(this.dao.getAll()).willReturn(this.tasks);

        this.vs.execValidations();

        assertEquals(1, this.tasks.get(0).reqHeaders().size());
        verify(this.ns).sendVTaskErrorsNotification(anyList());
    }

    @Test
    void execValidationsLogsValidTaskResult() throws Exception {
        given(this.res.body()).willReturn("");
        given(this.cl.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .willReturn(CompletableFuture.completedFuture(this.res));

        this.tasks.add(
            new ValidationTask(GET, "http://localhost", emptyList(), this.reqBody,200, "")
        );
        given(this.dao.getAll()).willReturn(this.tasks);

        this.vs.execValidations();

        assertThat(this.dao.getAll().get(0).reqHeaders().isEmpty()).isTrue();
        verify(this.logger).info(anyString());
    }

    @Test
    void execValidationsLogsInvalidTaskResult() throws Exception {
        given(this.res.body()).willReturn(null);
        given(this.cl.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .willReturn(CompletableFuture.completedFuture(this.res));

        this.tasks.add(
            new ValidationTask(GET, "http://localhost", emptyList(), this.reqBody,200, "")
        );
        given(this.dao.getAll()).willReturn(this.tasks);

        this.vs.execValidations();

        assertThat(this.dao.getAll()).isNotEmpty();
        verify(this.logger).info(anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-", "@daily"})
    void whenDashOrDailyMacroCronExpressionIsValidConfigReturnsTrue(String s) {
        given(this.env.getProperty(anyString())).willReturn(s);

        assertThat(this.vs.isValidConfig()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"*"})
    void whenInvalidCronExpressionIsValidConfigReturnsFalse(String s) {
        assertThat(this.vs.isValidCronExpression(s)).isFalse();
    }

    @Test
    void whenLrEndIsNullGetLastRunInfoReturnsEmptyMap() {
        assertThat(this.vs.getLastRunInfo()).isEmpty();
    }

    @Test
    void whenLrEndIsNotNullGetLastRunInfoReturnsLastRunData() throws Exception {
        given(this.res.body()).willReturn("");
        given(this.cl.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .willReturn(CompletableFuture.completedFuture(this.res));

        this.tasks.add(
            new ValidationTask(GET, "http://localhost", emptyList(), this.reqBody,200, "")
        );
        given(this.dao.getAll()).willReturn(this.tasks);

        this.vs.execValidations();

        Map<String, String> res = this.vs.getLastRunInfo();

        assertThat(res.containsKey(START_TIME_KEY)).isTrue();
        assertThat(res.containsKey(TIME_ELAPSED_KEY)).isTrue();
        assertThat(res.containsKey(TASKS_TOTAL_KEY)).isTrue();
        assertThat(res.containsKey(TASKS_OK_KEY)).isTrue();
        assertThat(res.containsKey(TASKS_FAILED_KEY)).isTrue();
        assertThat(res.get(TASKS_TOTAL_KEY)).isEqualTo(String.valueOf(1));
    }
}
