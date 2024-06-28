package cf.maybelambda.httpvalidator.springboot.controller;

import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import cf.maybelambda.httpvalidator.springboot.service.EmailNotificationService;
import cf.maybelambda.httpvalidator.springboot.service.ValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppInfoController.class)
class AppInfoControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AppInfoController ctrl;
    @MockBean
    XMLValidationTaskDao dao;
    @MockBean
    EmailNotificationService mailServ;
    @MockBean
    ValidationService valServ;

    @Test
    void informWebAppStatusReturns200WhenNoInitErrors() throws Exception {
        assertThat(this.ctrl.informWebAppStatus().getStatusCode()).isEqualTo(HttpStatusCode.valueOf(200));

        this.mockMvc.perform(get(AppInfoController.STATUS_ENDPOINT)).andExpect(status().isOk());
    }
}
