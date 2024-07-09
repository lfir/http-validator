package cf.maybelambda.httpvalidator.springboot.controller;

import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import cf.maybelambda.httpvalidator.springboot.service.EmailNotificationService;
import cf.maybelambda.httpvalidator.springboot.service.EventListenerService;
import cf.maybelambda.httpvalidator.springboot.service.JwtAuthenticationService;
import cf.maybelambda.httpvalidator.springboot.service.ValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.CONFIG_STATUS_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.DATAFILE_STATUS_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.ERROR_VALUE;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.NO_LASTRUN_DATA_ERROR_MSG;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.OK_VALUE;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.START_TIME_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TASKS_TOTAL_KEY;
import static cf.maybelambda.httpvalidator.springboot.filter.JwtRequestFilter.AUTHORIZATION_HEADER_KEY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppInfoController.class)
public class AppInfoControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private XMLValidationTaskDao dao;
    @MockBean
    private EmailNotificationService mailServ;
    @MockBean
    private ValidationService valServ;
    @MockBean
    private EventListenerService eventServ;
    @MockBean
    private JwtAuthenticationService authServ;

    @Test
    void informWebAppStatusReturns200AndJSONStatusDataWhenNoInitErrors() throws Exception {
        given(this.eventServ.getStartDateTime()).willReturn("2001");
        given(this.authServ.isValidToken(anyString())).willReturn(true);

        this.mockMvc.perform(get(AppInfoController.STATUS_ENDPOINT)
            .header(AUTHORIZATION_HEADER_KEY, "testToken"))

            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$." + START_TIME_KEY).isString()
        );
    }

    @Test
    void informWebAppStatusReturnsDataFileStatusOkWhenParserInformsStatustOk() throws Exception {
        given(this.dao.isDataFileStatusOk()).willReturn(true);
        given(this.authServ.isValidToken(anyString())).willReturn(true);

        this.mockMvc.perform(get(AppInfoController.STATUS_ENDPOINT)
            .header(AUTHORIZATION_HEADER_KEY, "testToken"))

            .andExpect(jsonPath("$." + DATAFILE_STATUS_KEY).value(OK_VALUE)
        );
    }

    @Test
    void informWebAppStatusReturnsDataFileErrorStatusWhenParserInformsStatusNotOk() throws Exception {
        given(this.dao.isDataFileStatusOk()).willReturn(false);
        given(this.authServ.isValidToken(anyString())).willReturn(true);

        this.mockMvc.perform(get(AppInfoController.STATUS_ENDPOINT)
            .header(AUTHORIZATION_HEADER_KEY, "testToken"))

            .andExpect(jsonPath("$." + DATAFILE_STATUS_KEY).value(ERROR_VALUE)
        );
    }

    @Test
    void informWebAppStatusReturnsConfigErrorStatusWhenServicesHaveInvalidConfig() throws Exception {
        given(this.mailServ.isValidConfig()).willReturn(true);
        given(this.valServ.isValidConfig()).willReturn(false);
        given(this.authServ.isValidToken(anyString())).willReturn(true);

        this.mockMvc.perform(get(AppInfoController.STATUS_ENDPOINT)
            .header(AUTHORIZATION_HEADER_KEY, "testToken"))

            .andExpect(jsonPath("$." + CONFIG_STATUS_KEY).value(ERROR_VALUE)
        );
    }

    @Test
    void informWebAppStatusReturnsConfigOKStatusWhenServicesHaveValidConfig() throws Exception {
        given(this.mailServ.isValidConfig()).willReturn(true);
        given(this.valServ.isValidConfig()).willReturn(true);
        given(this.authServ.isValidToken(anyString())).willReturn(true);

        this.mockMvc.perform(get(AppInfoController.STATUS_ENDPOINT)
            .header(AUTHORIZATION_HEADER_KEY, "testToken"))

            .andExpect(jsonPath("$." + CONFIG_STATUS_KEY).value(OK_VALUE)
        );
    }

    @Test
    void informLastRunDataReturns503ServiceUnavailableWhenNoLastRunDataIsAvailable() throws Exception {
        Map<String, String> res = new HashMap<>();
        given(this.valServ.getLastRunInfo()).willReturn(res);
        given(this.authServ.isValidToken(anyString())).willReturn(true);

        this.mockMvc.perform(get(AppInfoController.LAST_RUN_ENDPOINT)
            .header(AUTHORIZATION_HEADER_KEY, "testToken"))

            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$." + ERROR_VALUE.toLowerCase()).value(NO_LASTRUN_DATA_ERROR_MSG)
        );
    }

    @Test
    void informWebAppStatusReturns401UnauthorizedWhenInvalidJWTInRequestHeader() throws Exception {
        given(this.dao.isDataFileStatusOk()).willReturn(true);
        given(this.authServ.isValidToken(anyString())).willReturn(false);

        this.mockMvc.perform(get(AppInfoController.STATUS_ENDPOINT)
            .header(AUTHORIZATION_HEADER_KEY, "invalidToken"))

            .andExpect(status().isUnauthorized()
        );
    }

    @Test
    void informLastRunDataReturns200AndTotalTasksWhenLastRunDataIsAvailable() throws Exception {
        Map<String, String> res = new HashMap<>();
        res.put(TASKS_TOTAL_KEY, "3");
        given(this.valServ.getLastRunInfo()).willReturn(res);
        given(this.authServ.isValidToken(anyString())).willReturn(true);

        this.mockMvc.perform(get(AppInfoController.LAST_RUN_ENDPOINT)
            .header(AUTHORIZATION_HEADER_KEY, "testToken"))

            .andExpect(status().isOk())
            .andExpect(jsonPath("$." + TASKS_TOTAL_KEY).value("3")
        );
    }
}
