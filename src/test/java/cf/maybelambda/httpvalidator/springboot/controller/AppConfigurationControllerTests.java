package cf.maybelambda.httpvalidator.springboot.controller;

import cf.maybelambda.httpvalidator.springboot.HTTPValidatorWebApp;
import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import cf.maybelambda.httpvalidator.springboot.service.ValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static cf.maybelambda.httpvalidator.springboot.controller.AppConfigurationController.CRON_EXPRESSION_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppConfigurationController.INVALID_CRON_EXPRESSION_ERROR_MSG;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.ERROR_VALUE;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppConfigurationController.class)
public class AppConfigurationControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;
    @MockBean
    private ValidationService valServ;
    @MockBean
    private XMLValidationTaskDao dao;
    private final Map<String, String> reqBody = new HashMap<>();

    @BeforeEach
    public void setUp() {
        reqBody.put(CRON_EXPRESSION_KEY, "?");
    }

    @Test
    void updateValidatorRunScheduleReturns400BadRequestWhenInvalidCronExpressionIsReceived() throws Exception {
        given(this.valServ.isValidCronExpression(anyString())).willReturn(false);

        this.mockMvc.perform(
            put(AppConfigurationController.UPD_RUN_SCHEDULE_ENDPOINT)
                .content(mapper.writeValueAsString(this.reqBody))
                .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$." + ERROR_VALUE.toLowerCase()).value(INVALID_CRON_EXPRESSION_ERROR_MSG)
        );
    }

    @Test
    void updateValidatorRunScheduleReturns200AndCallsAppRestartWhenValidCronExpressionIsReceived() throws Exception {
        MockedStatic<HTTPValidatorWebApp> app = mockStatic(HTTPValidatorWebApp.class);
        given(this.valServ.isValidCronExpression(anyString())).willReturn(true);

        this.mockMvc.perform(
            put(AppConfigurationController.UPD_RUN_SCHEDULE_ENDPOINT)
                .content(mapper.writeValueAsString(this.reqBody))
                .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk()
            );

        app.verify(() -> HTTPValidatorWebApp.restartAppContextWithNewRunSchedule(anyString()));
        app.close();
    }
}
