package cf.maybelambda.httpvalidator.springboot.itest;

import cf.maybelambda.httpvalidator.springboot.service.JwtAuthenticationService;
import cf.maybelambda.httpvalidator.springboot.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.headers.RequestHeadersSnippet;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.CONFIG_STATUS_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.DATAFILE_STATUS_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.ERROR_VALUE;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.LAST_RUN_ENDPOINT;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.NO_LASTRUN_DATA_ERROR_MSG;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.START_TIME_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.STATUS_ENDPOINT;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TASKS_FAILED_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TASKS_OK_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TASKS_TOTAL_KEY;
import static cf.maybelambda.httpvalidator.springboot.controller.AppInfoController.TIME_ELAPSED_KEY;
import static cf.maybelambda.httpvalidator.springboot.filter.JwtRequestFilter.AUTHORIZATION_HEADER_KEY;
import static cf.maybelambda.httpvalidator.springboot.service.JwtAuthenticationService.BEARER_PREFIX;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ExtendWith({SpringExtension.class, RestDocumentationExtension.class})
@ActiveProfiles("test")
@SpringBootTest
public class AppInfoControllerIntegrationTests {
    public static final RequestHeadersSnippet REQUEST_HEADERS_SNIPPET = requestHeaders(
        headerWithName(AUTHORIZATION_HEADER_KEY).description("Bearer token for authentication")
    );
    public static final String START_TIME_DESCR = "The start time of the application in a formatted string";
    public static final String START_TIME_LR_DESCR = "The start time of the last run in a formatted string";
    public static final String DATAFILE_STATUS_DESCR = """
            `OK`: Data file status is okay, i.e. it exists and is readable
            
            `ERROR`: Data file status is not okay""";
    public static final String CONFIG_STATUS_DESCR = """
            `OK`: Both mail and validation configurations are okay (run schedule, credentials, from and to addresses)
            
            `ERROR`: Either mail or validation configuration is not okay""";
    public static final String TIME_ELAPSED_DESCR = "Duration of the last run";
    public static final String TASKS_TOTAL_DESCR = "The total number of tasks to be processed in the last run";
    public static final String TASKS_OK_DESCR = "The number of tasks with expected results";
    public static final String TASKS_FAILED_DESCR = "The number of tasks with invalid results in the last run";
    private String testsToken;
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JwtAuthenticationService authServ;
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
    public void canGetAppStatus() throws Exception {
        this.mockMvc.perform(get(STATUS_ENDPOINT)
            .header(AUTHORIZATION_HEADER_KEY, this.testsToken)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())

            .andDo(
                document("{method-name}",
                    REQUEST_HEADERS_SNIPPET,
                    responseFields(
                        fieldWithPath(START_TIME_KEY).description(START_TIME_DESCR),
                        fieldWithPath(DATAFILE_STATUS_KEY).description(DATAFILE_STATUS_DESCR),
                        fieldWithPath(CONFIG_STATUS_KEY).description(CONFIG_STATUS_DESCR)
                    )
                )
            );
    }

    @Test
    public void error503WhenNoLastRunDataAvailable() throws Exception {
        this.mockMvc.perform(get(LAST_RUN_ENDPOINT)
            .header(AUTHORIZATION_HEADER_KEY, this.testsToken)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isServiceUnavailable())

            .andDo(
                document("{method-name}",
                    REQUEST_HEADERS_SNIPPET,
                    responseFields(
                        fieldWithPath(ERROR_VALUE.toLowerCase()).description(NO_LASTRUN_DATA_ERROR_MSG)
                    )
                )
            );
    }

    @Test
    public void canGetLastRunInfoWhenDataAvailable() throws Exception {
        this.valServ.execValidations();

        this.mockMvc.perform(get(LAST_RUN_ENDPOINT)
            .header(AUTHORIZATION_HEADER_KEY, this.testsToken)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())

            .andDo(
                document("{method-name}",
                    REQUEST_HEADERS_SNIPPET,
                    responseFields(
                        fieldWithPath(START_TIME_KEY).description(START_TIME_LR_DESCR),
                        fieldWithPath(TIME_ELAPSED_KEY).description(TIME_ELAPSED_DESCR),
                        fieldWithPath(TASKS_TOTAL_KEY).description(TASKS_TOTAL_DESCR),
                        fieldWithPath(TASKS_OK_KEY).description(TASKS_OK_DESCR),
                        fieldWithPath(TASKS_FAILED_KEY).description(TASKS_FAILED_DESCR)
                    )
                )
            );
    }
}
