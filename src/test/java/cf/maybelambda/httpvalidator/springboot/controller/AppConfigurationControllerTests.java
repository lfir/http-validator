package cf.maybelambda.httpvalidator.springboot.controller;

import cf.maybelambda.httpvalidator.springboot.HTTPValidatorWebApp;
import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import cf.maybelambda.httpvalidator.springboot.service.JwtAuthenticationService;
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

import javax.management.modelmbean.XMLParseException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static cf.maybelambda.httpvalidator.springboot.controller.AppConfigurationController.CRON_EXPRESSION_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppConfigurationController.INVALID_CRON_EXPRESSION_ERROR_MSG;
import static cf.maybelambda.httpvalidator.springboot.controller.AppConfigurationController.INVALID_DATA_FILE_ERROR_MSG;
import static cf.maybelambda.httpvalidator.springboot.controller.AppConfigurationController.UPD_DATA_FILE_ENDPOINT;
import static cf.maybelambda.httpvalidator.springboot.controller.AppConfigurationController.UPD_DATA_FILE_ERROR_MSG;
import static cf.maybelambda.httpvalidator.springboot.controller.AppConfigurationController.UPD_RUN_SCHEDULE_ENDPOINT;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.ERROR_VALUE;
import static cf.maybelambda.httpvalidator.springboot.filter.JwtRequestFilter.AUTHORIZATION_HEADER_KEY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
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
    @MockBean
    private JwtAuthenticationService authServ;
    private final Map<String, String> reqBody = new HashMap<>();

    @BeforeEach
    public void setUp() {
        reqBody.put(CRON_EXPRESSION_KEY, "?");
        given(this.authServ.isValidToken(anyString())).willReturn(true);
    }

    @Test
    void updateValidatorRunScheduleReturns400BadRequestWhenInvalidCronExpressionIsReceived() throws Exception {
        given(this.valServ.isValidCronExpression(anyString())).willReturn(false);

        this.mockMvc.perform(
            put(UPD_RUN_SCHEDULE_ENDPOINT)
                .header(AUTHORIZATION_HEADER_KEY, "testToken")
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
            put(UPD_RUN_SCHEDULE_ENDPOINT)
                .header(AUTHORIZATION_HEADER_KEY, "testToken")
                .content(mapper.writeValueAsString(this.reqBody))
                .contentType(MediaType.APPLICATION_JSON))

                .andExpect(status().isOk()
            );

        app.verify(() -> HTTPValidatorWebApp.restartAppContextWithNewRunSchedule(anyString()));
        app.close();
    }

    @Test
    void updateValidatorDataFileReturns400BadRequestWhenParserCannotParseXML() throws Exception {
        // TODO: Use a mock of a file instead of null as argument
        // but doThrow only takes an instance of MultipartFile as param for this.dao.updateDataFile
        // and mockMvc.perform(multipart... only accepts an instance of MockMultipartFile
        doThrow(XMLParseException.class).when(this.dao).updateDataFile(null);

        this.mockMvc.perform(
            put(UPD_DATA_FILE_ENDPOINT)
                .header(AUTHORIZATION_HEADER_KEY, "testToken"))

            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$." + ERROR_VALUE.toLowerCase()).value(INVALID_DATA_FILE_ERROR_MSG)
        );
    }

    @Test
    void updateValidatorDataFileReturns500InternalServerErrorWhenDataFileCannotBeUpdated() throws Exception {
        doThrow(IOException.class).when(this.dao).updateDataFile(null);

        this.mockMvc.perform(
            put(UPD_DATA_FILE_ENDPOINT)
                .header(AUTHORIZATION_HEADER_KEY, "testToken"))

            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$." + ERROR_VALUE.toLowerCase()).value(UPD_DATA_FILE_ERROR_MSG)
        );
    }

    @Test
    void updateValidatorDataFileReturns200WhenDataFileIsUpdated() throws Exception {
        this.mockMvc.perform(
            put(UPD_DATA_FILE_ENDPOINT)
                .header(AUTHORIZATION_HEADER_KEY, "testToken"))

            .andExpect(status().isOk()
        );
    }
}
