package cf.maybelambda.httpvalidator.springboot.persistence;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
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
import java.util.List;
import java.util.regex.Pattern;

import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.DATAFILE_PROPERTY;
import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.HEADER_DELIMITER;
import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.REQ_HEADERS_ATTR;
import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.REQ_METHOD_ATTR;
import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.REQ_URL_ATTR;
import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.RES_BODY_ATTR;
import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.RES_SC_ATTR;
import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.VALIDATION_TAG;
import static cf.maybelambda.httpvalidator.springboot.service.ValidationService.HEADER_KEY_VALUE_DELIMITER;
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
    private final Logger logger = mock(Logger.class);
    private final DocumentBuilder xmlParser = mock(DocumentBuilder.class);
    private final Document doc = mock(Document.class);
    private final NodeList nodes = mock(NodeList.class);
    private final Node nodeA = mock(Node.class);
    private final Node nodeB = mock(Node.class);
    private final Node validationNode = mock(Node.class);
    private final NamedNodeMap nm = mock(NamedNodeMap.class);
    private final Environment env = mock(Environment.class);
    private XMLValidationTaskDao taskDao;

    @BeforeEach
    void setUp() throws Exception {
        this.taskDao = new XMLValidationTaskDao();
        this.taskDao.setXmlParser(this.xmlParser);
        this.taskDao.setLogger(logger);
        this.taskDao.setEnv(this.env);

        given(this.env.getProperty(DATAFILE_PROPERTY)).willReturn("/dev/null");
        given(this.nodes.item(anyInt())).willReturn(this.validationNode);
        given(validationNode.getAttributes()).willReturn(this.nm);

        given(this.nm.getNamedItem(REQ_METHOD_ATTR)).willReturn(this.nodeA);
        given(this.nm.getNamedItem(REQ_URL_ATTR)).willReturn(this.nodeA);
        given(this.nm.getNamedItem(REQ_HEADERS_ATTR)).willReturn(nodeB);
        given(this.nm.getNamedItem(RES_SC_ATTR)).willReturn(this.nodeA);
        given(this.nm.getNamedItem(RES_BODY_ATTR)).willReturn(this.nodeB);

        given(this.nodeA.getTextContent()).willReturn("0");
    }

    @Test
    void xmlWithNoValidationTagsProducesZeroTasks() throws Exception {
        given(this.doc.getElementsByTagName(anyString())).willReturn(this.nodes);
        given(this.xmlParser.parse(any(InputStream.class))).willReturn(this.doc);
        given(this.nodes.getLength()).willReturn(0);

        assertThat(this.taskDao.getAll().isEmpty()).isTrue();
    }

    @Test
    void taskDataIsReadWhenWellFormedXMLParsedWithoutErrors() throws Exception {
        given(this.doc.getElementsByTagName(VALIDATION_TAG)).willReturn(this.nodes);
        given(this.xmlParser.parse(any(InputStream.class))).willReturn(this.doc);
        given(this.nodes.getLength()).willReturn(1);

        given(this.nodeB.getTextContent()).willReturn(
            String.format("X-H1%sB32C%sH2%s456", HEADER_KEY_VALUE_DELIMITER, HEADER_DELIMITER, HEADER_KEY_VALUE_DELIMITER)
        );

        List<ValidationTask> ans = this.taskDao.getAll();

        assertEquals(Integer.parseInt(this.nodeA.getTextContent()), ans.get(0).reqMethod());
        assertEquals(nodeA.getTextContent(), ans.get(0).reqURL());
        assertEquals(this.nodeB.getTextContent().split(Pattern.quote(HEADER_DELIMITER))[0], ans.get(0).reqHeaders().get(0));
        assertEquals(this.nodeB.getTextContent().split(Pattern.quote(HEADER_DELIMITER))[1], ans.get(0).reqHeaders().get(1));
        assertEquals(Integer.parseInt(this.nodeA.getTextContent()), ans.get(0).validStatusCode());
        assertEquals(this.nodeB.getTextContent(), ans.get(0).validBody());
    }

    @Test
    void xmlAttributesThatCanBeEmptyInDatafileAreParsedOk() throws Exception {
        given(this.doc.getElementsByTagName(VALIDATION_TAG)).willReturn(this.nodes);
        given(this.xmlParser.parse(any(InputStream.class))).willReturn(this.doc);
        given(this.nodes.getLength()).willReturn(1);

        given(this.nodeB.getTextContent()).willReturn("");

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
