package cf.maybelambda.httpvalidator.springboot.controller;

import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import cf.maybelambda.httpvalidator.springboot.service.EmailNotificationService;
import cf.maybelambda.httpvalidator.springboot.service.ValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.management.modelmbean.XMLParseException;

import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.CONFIG_STATUS_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.DATAFILE_STATUS_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.ERROR_VALUE;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.OK_VALUE;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.START_TIME_KEY;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppInfoController.class)
class AppInfoControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    XMLValidationTaskDao dao;
    @MockBean
    EmailNotificationService mailServ;
    @MockBean
    ValidationService valServ;

    @Test
    void informWebAppStatusReturns200AndJSONStatusDataWhenNoInitErrors() throws Exception {
        this.mockMvc.perform(get(AppInfoController.STATUS_ENDPOINT))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$." + START_TIME_KEY).isString())
            .andExpect(jsonPath("$." + DATAFILE_STATUS_KEY).value(OK_VALUE))
            .andExpect(jsonPath("$." + CONFIG_STATUS_KEY).value(ERROR_VALUE)
        );
    }

    @Test
    void informWebAppStatusReturnsDataFileErrorStatusWhenParserThrowsException() throws Exception {
        given(this.dao.getDocData()).willThrow(XMLParseException.class);

        this.mockMvc.perform(get(AppInfoController.STATUS_ENDPOINT))
            .andExpect(jsonPath("$." + DATAFILE_STATUS_KEY).value(ERROR_VALUE)
        );
    }
}
