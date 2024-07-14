package cf.maybelambda.httpvalidator.springboot.itest;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao;
import cf.maybelambda.httpvalidator.springboot.service.JwtAuthenticationService;
import cf.maybelambda.httpvalidator.springboot.service.ValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static cf.maybelambda.httpvalidator.springboot.controller.AppConfigurationController.CRON_EXPRESSION_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppConfigurationController.INVALID_CRON_EXPRESSION_ERROR_MSG;
import static cf.maybelambda.httpvalidator.springboot.controller.AppConfigurationController.INVALID_DATA_FILE_ERROR_MSG;
import static cf.maybelambda.httpvalidator.springboot.controller.AppConfigurationController.UPD_DATA_FILE_ENDPOINT;
import static cf.maybelambda.httpvalidator.springboot.controller.AppConfigurationController.UPD_DATA_FILE_ERROR_MSG;
import static cf.maybelambda.httpvalidator.springboot.controller.AppConfigurationController.UPD_RUN_SCHEDULE_ENDPOINT;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.ERROR_VALUE;
import static cf.maybelambda.httpvalidator.springboot.itest.AppInfoControllerIntegrationTests.REQUEST_HEADERS_SNIPPET;
import static cf.maybelambda.httpvalidator.springboot.service.JwtAuthenticationService.BEARER_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ExtendWith({SpringExtension.class, RestDocumentationExtension.class})
@ActiveProfiles("test")
@SpringBootTest
public class AppConfigurationControllerIntegrationTests {
    public static String UPD_DATAFILE_ERRORS_DESCR = INVALID_DATA_FILE_ERROR_MSG + " or " + UPD_DATA_FILE_ERROR_MSG;
    private String testsToken;
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JwtAuthenticationService authServ;
    @Autowired
    private XMLValidationTaskDao dao;
    @Autowired
    private ValidationService valServ;

    @BeforeEach
    public void setUp(RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = webAppContextSetup(this.context)
            .apply(documentationConfiguration(restDocumentation))
            .alwaysDo(document("{method-name}", preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())))
            .build();

        this.testsToken = BEARER_PREFIX + this.authServ.getNewTokenValidFor(1);
    }

    @Test
    public void error400WhenUpdateRunScheduleRequestWithInvalidCronExpression() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put(CRON_EXPRESSION_KEY, "* 2 * ?");

        this.mockMvc.perform(put(UPD_RUN_SCHEDULE_ENDPOINT)
            .header(HttpHeaders.AUTHORIZATION, this.testsToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(this.mapper.writeValueAsString(requestBody)))
            .andExpect(status().isBadRequest())

            .andDo(
                document("{method-name}",
                    REQUEST_HEADERS_SNIPPET,
                    requestFields(
                        fieldWithPath(CRON_EXPRESSION_KEY).description("A valid cron expression for scheduling tasks")
                    ),
                    responseFields(
                        fieldWithPath(ERROR_VALUE.toLowerCase()).description(INVALID_CRON_EXPRESSION_ERROR_MSG)
                    )
                )
            );

        // Verify that the invalid expression was not applied
        assertThat(this.valServ.isValidConfig()).isTrue();
    }

    @Test
    public void canUpdateDataFileWithValidXML() throws Exception {
        ValidationTask task = new ValidationTask(0, "http://example.com/api/test", Collections.emptyList(), 200, "");

        String xmlContent =   "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<validations>"
                            +    "<validation reqmethod=\"0\" requrl=\"http://example.com/api/test\" "
                            +        "reqheaders=\"\" ressc=\"200\" resbody=\"\"/>"
                            + "</validations>";

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "validations.xml",
            MediaType.APPLICATION_XML_VALUE,
            xmlContent.getBytes(StandardCharsets.UTF_8)
        );

        this.mockMvc.perform(multipart(HttpMethod.PUT, UPD_DATA_FILE_ENDPOINT)
            .file(file)
            .header(HttpHeaders.AUTHORIZATION, this.testsToken))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.blankString()))

            .andDo(document("{method-name}", REQUEST_HEADERS_SNIPPET));

        assertThat(this.dao.isDataFileStatusOk()).isTrue();
        assertThat(this.dao.getAll().get(0)).isEqualTo(task);
    }

    @Test
    public void error400WhenUpdateDataFileRequestWithInvalidXML() throws Exception {
        ValidationTask task = this.dao.getAll().get(0);

        String xmlContent =   "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<validations>"
                            +    "<validation reqmethod=\"25\" requrl=\"http://example.com/api/test\" "
                            +        "reqheaders=\"\" ressc=\"200\" resbody=\"\"/>"
                            + "</validations>";

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "validations.xml",
            MediaType.APPLICATION_XML_VALUE,
            xmlContent.getBytes(StandardCharsets.UTF_8)
        );

        this.mockMvc.perform(multipart(HttpMethod.PUT, UPD_DATA_FILE_ENDPOINT)
            .file(file)
            .header(HttpHeaders.AUTHORIZATION, this.testsToken))
            .andExpect(status().isBadRequest())

            .andDo(
                document("{method-name}",
                    REQUEST_HEADERS_SNIPPET,
                    responseFields(
                        fieldWithPath(ERROR_VALUE.toLowerCase()).description(UPD_DATAFILE_ERRORS_DESCR)
                    )
                )
            );

        // Verify the original file contents have not been modified
        assertThat(this.dao.getAll().get(0)).isEqualTo(task);
    }
}
