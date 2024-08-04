package cf.maybelambda.httpvalidator.springboot.persistence;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.management.modelmbean.XMLParseException;
import javax.xml.parsers.DocumentBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.DATAFILE_PROPERTY;
import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.HEADER_TAG;
import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.REQ_BODY_TAG;
import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.REQ_METHOD_ATTR;
import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.RES_SC_ATTR;
import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.RES_TAG;
import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.URL_TAG;
import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.VALIDATION_TAG;
import static javax.swing.text.html.FormSubmitEvent.MethodType.GET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

public class XMLValidationTaskDaoTests {
    private final ObjectMapper mapper = mock(ObjectMapper.class);
    private final JsonNode parsedReqBody = mock(JsonNode.class);
    private final Logger logger = mock(Logger.class);
    private final DocumentBuilder xmlParser = mock(DocumentBuilder.class);
    private final Document doc = mock(Document.class);
    private final Environment env = mock(Environment.class);
    private final NodeList nodes = mock(NodeList.class);
    private final Node validationNode = mock(Node.class);
    private final NodeList childNodes = mock(NodeList.class);
    private final Node url = mock(Node.class);
    private final NamedNodeMap urlAttrs = mock(NamedNodeMap.class);
    private final Node nodeA = mock(Node.class);
    private final Node header = mock(Node.class);
    private final Node reqbody = mock(Node.class);
    private final Node response = mock(Node.class);
    private final NamedNodeMap resBodyAttrs = mock(NamedNodeMap.class);
    private final Node nodeB = mock(Node.class);
    private XMLValidationTaskDao taskDao;

    @BeforeEach
    void setUp() throws Exception {
        this.taskDao = new XMLValidationTaskDao();
        this.taskDao.setXmlParser(this.xmlParser);
        this.taskDao.setLogger(logger);

        given(this.mapper.nullNode()).willReturn(this.parsedReqBody);
        this.taskDao.setObjectMapper(this.mapper);

        given(this.env.getProperty(DATAFILE_PROPERTY)).willReturn("/dev/null");
        this.taskDao.setEnv(this.env);

        // Get Document from XML
        given(this.doc.getElementsByTagName(VALIDATION_TAG)).willReturn(this.nodes);
        given(this.xmlParser.parse(any(InputStream.class))).willReturn(this.doc);
        // Get <validation> node from <validations>
        given(this.nodes.item(anyInt())).willReturn(this.validationNode);
        // Get <validation>'s child nodes
        given(this.validationNode.getChildNodes()).willReturn(this.childNodes);
        // <url>
        given(this.childNodes.item(0)).willReturn(this.url);
        given(this.url.getNodeName()).willReturn(URL_TAG);
        given(this.url.getTextContent()).willReturn("http://localhost:8080");
        given(this.url.getAttributes()).willReturn(this.urlAttrs);
        given(this.urlAttrs.getNamedItem(REQ_METHOD_ATTR)).willReturn(this.nodeA);
        given(this.nodeA.getTextContent()).willReturn("0");
        // <response>
        given(this.response.getNodeName()).willReturn(RES_TAG);
        given(this.response.getAttributes()).willReturn(this.resBodyAttrs);
        given(this.resBodyAttrs.getNamedItem(RES_SC_ATTR)).willReturn(this.nodeB);
        given(this.nodeB.getTextContent()).willReturn("200");
    }

    @Test
    void xmlWithNoValidationTagsProducesZeroTasks() throws Exception {
        // Number of <validation> elements
        given(this.nodes.getLength()).willReturn(0);

        assertThat(this.taskDao.getAll().isEmpty()).isTrue();
    }

    @Test
    void taskDataIsReadWhenWellFormedXMLParsedWithoutErrors() throws Exception {
        // Number of <validation> elements
        given(this.nodes.getLength()).willReturn(1);
        // Number of <validation>'s child nodes
        given(this.childNodes.getLength()).willReturn(4);
        // <header>
        given(this.childNodes.item(1)).willReturn(this.header);
        given(this.header.getNodeName()).willReturn(HEADER_TAG);
        given(this.header.getTextContent()).willReturn("Content-Type|text/plain");
        // <reqbody>
        given(this.childNodes.item(2)).willReturn(this.reqbody);
        given(this.reqbody.getNodeName()).willReturn(REQ_BODY_TAG);
        given(this.reqbody.getTextContent()).willReturn("\"data\":[]");
        given(this.mapper.readTree(anyString())).willReturn(this.parsedReqBody);
        // <response>
        given(this.response.getTextContent()).willReturn("valid body");
        given(this.childNodes.item(3)).willReturn(this.response);

        List<ValidationTask> ans = this.taskDao.getAll();

        assertEquals(GET, ans.get(0).reqMethod());
        assertEquals(this.url.getTextContent(), ans.get(0).reqURL());
        assertEquals(this.header.getTextContent(), ans.get(0).reqHeaders().get(0));
        assertEquals(this.mapper.readTree(this.reqbody.getTextContent()), ans.get(0).reqBody());
        assertEquals(Integer.parseInt(this.resBodyAttrs.getNamedItem(RES_SC_ATTR).getTextContent()), ans.get(0).validStatusCode());
        assertEquals(this.response.getTextContent(), ans.get(0).validBody());
    }

    @Test
    void xmlAttributesThatCanBeEmptyInDatafileAreParsedOk() throws Exception {
        // Number of <validation> elements
        given(this.nodes.getLength()).willReturn(1);
        // 2 child nodes of <validation>. No <header> or <reqbody> elements present
        given(this.childNodes.getLength()).willReturn(2);
        // <response />
        given(this.response.getTextContent()).willReturn("");
        given(this.childNodes.item(1)).willReturn(this.response);

        List<ValidationTask> ans = this.taskDao.getAll();

        assertThat(ans.get(0).reqHeaders().isEmpty()).isTrue();
        assertEquals("", ans.get(0).validBody());
    }

    @Test
    void whenGetAllThrowsSAXExceptionThenErrorIsLogged() throws Exception {
        given(this.xmlParser.parse(any(InputStream.class))).willThrow(SAXException.class);

        assertThrows(XMLParseException.class, () -> this.taskDao.getAll());
        verify(logger).error(anyString(), any(Throwable.class));
    }

    @Test
    void whenJacksonThrowsJSONExceptionInGetAllThenErrorIsLogged() throws Exception {
        // Number of <validation> elements
        given(this.nodes.getLength()).willReturn(1);
        // 2 child nodes of <validation>: <url> and <reqbody>
        given(this.childNodes.getLength()).willReturn(2);
        // <reqbody>
        given(this.childNodes.item(1)).willReturn(this.reqbody);
        given(this.reqbody.getNodeName()).willReturn(REQ_BODY_TAG);
        given(this.reqbody.getTextContent()).willReturn("");
        given(this.mapper.readTree(anyString())).willThrow(JsonProcessingException.class);

        assertThrows(XMLParseException.class, () -> this.taskDao.getAll());
        verify(logger).error(anyString(), any(Throwable.class));
    }

    @Test
    void getAllReturnsTasksFromMemoryWhenDataFileNotModifiedSinceLastRun() throws Exception {
        this.taskDao.setLastModifiedTime(Instant.now().plusSeconds(86400).toEpochMilli());

        assertThat(this.taskDao.getAll()).isNull();
    }

    @Test
    void isDataFileStatusOkReturnsTrueWhenDataFileIsRegularFileAndReadable() {
        try (MockedStatic<Files> classMock = mockStatic(Files.class)) {
            classMock.when(() -> Files.isRegularFile(any(Path.class))).thenReturn(true);
            classMock.when(() -> Files.isReadable(any(Path.class))).thenReturn(true);

            assertThat(this.taskDao.isDataFileStatusOk()).isTrue();
        }
    }

    @Test
    void isDataFileStatusOkReturnsFalseWhenDataFileIsNotRegularFileOrNotReadable() {
        try (MockedStatic<Files> classMock = mockStatic(Files.class)) {
            classMock.when(() -> Files.isRegularFile(any(Path.class))).thenReturn(false);
            classMock.when(() -> Files.isReadable(any(Path.class))).thenReturn(false);
            assertThat(this.taskDao.isDataFileStatusOk()).isFalse();

            classMock.when(() -> Files.isRegularFile(any(Path.class))).thenReturn(true);
            classMock.when(() -> Files.isReadable(any(Path.class))).thenReturn(false);
            assertThat(this.taskDao.isDataFileStatusOk()).isFalse();

            classMock.when(() -> Files.isRegularFile(any(Path.class))).thenReturn(false);
            classMock.when(() -> Files.isReadable(any(Path.class))).thenReturn(true);
            assertThat(this.taskDao.isDataFileStatusOk()).isFalse();
        }
    }

    @Test
    void updateDataFileThrowsNPEWhenReceivedFileIsNull() {
        assertThrows(NullPointerException.class, () -> this.taskDao.updateDataFile(null));
        verify(logger).warn(anyString());
    }

    @Test
    void updateDataFileThrowsIOExceptionWhenDataCannotBeWrittenToDestinationFile() throws Exception {
        try (MockedStatic<Files> classMock = mockStatic(Files.class);
             InputStream is = InputStream.nullInputStream()) {
            MultipartFile file = mock(MultipartFile.class);

            given(file.getInputStream()).willReturn(is);
            given(file.getBytes()).willReturn(is.readAllBytes());
            classMock.when(() -> Files.write(any(Path.class), any(byte[].class))).thenThrow(IOException.class);

            assertThrows(IOException.class, () -> this.taskDao.updateDataFile(file));
            verify(logger).error(anyString(), any(Throwable.class));
        }
    }

    @Test
    void updateDataFileWritesReceivedFileDataToDestinationFile() throws Exception {
        try (InputStream is = InputStream.nullInputStream()) {
            MultipartFile file = mock(MultipartFile.class);
            given(file.getInputStream()).willReturn(is);
            given(file.getBytes()).willReturn(is.readAllBytes());

            this.taskDao.updateDataFile(file);

            verify(file).getInputStream();
            verify(file).getBytes();
        }
    }
}
