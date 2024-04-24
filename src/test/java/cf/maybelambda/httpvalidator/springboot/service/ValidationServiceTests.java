package cf.maybelambda.httpvalidator.springboot.service;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import org.junit.jupiter.api.Test;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
public class ValidationServiceTests {
    @Autowired
    private ValidationService vs;

    @Test
    void execValidationsSendsRequestAndNotificationViaHTTPAndEmailClients() throws IOException, InterruptedException {
        HttpClient cl = mock(HttpClient.class);
        given(cl.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).willReturn(mock(HttpResponse.class));
        this.vs.setClient(cl);

        EmailNotificationService ns = mock(EmailNotificationService.class);
        this.vs.setNotificationService(ns);

        XMLValidationTaskDao dao = mock(XMLValidationTaskDao.class);
        List<ValidationTask> tasks = new ArrayList<>();
        tasks.add(new ValidationTask(0, "http://localhost", Collections.emptyList(), 0, ""));
        given(dao.getAll()).willReturn(tasks);
        this.vs.setTaskReader(dao);

        this.vs.execValidations();

        verify(cl).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(ns).sendVTaskErrorsNotification(anyList());
    }
}
