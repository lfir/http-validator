package cf.maybelambda.httpvalidator.springboot.persistence;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.management.modelmbean.XMLParseException;
import javax.xml.parsers.DocumentBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
public class XMLValidationTaskDaoTests {
    private final DocumentBuilder xmlParser = mock(DocumentBuilder.class);
    private final Document doc = mock(Document.class);
    private final NodeList nodes = mock(NodeList.class);
    private final Node nodeA = mock(Node.class);
    private final Node nodeB = mock(Node.class);
    private final Node validationNode = mock(Node.class);
    private final NamedNodeMap nm = mock(NamedNodeMap.class);
    @Autowired
    private XMLValidationTaskDao taskDao;

    @BeforeEach
    void setUp() {
        this.taskDao.setXmlParser(this.xmlParser);

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
    void xmlWithNoValidationTagsProducesZeroTasks() throws IOException, SAXException, XMLParseException {
        given(doc.getElementsByTagName(anyString())).willReturn(this.nodes);
        given(this.xmlParser.parse(any(InputStream.class))).willReturn(this.doc);
        given(this.nodes.getLength()).willReturn(0);

        assertThat(this.taskDao.getAll().isEmpty()).isTrue();
    }

    @Test
    void taskDataIsReadWhenWellFormedXMLParsedWithoutErrors() throws IOException, SAXException, XMLParseException {
        given(this.doc.getElementsByTagName(VALIDATION_TAG)).willReturn(this.nodes);
        given(this.xmlParser.parse(any(InputStream.class))).willReturn(this.doc);
        given(this.nodes.getLength()).willReturn(1);

        given(this.nodeB.getTextContent()).willReturn(
            String.format("X-H1%sB32C%sH2%s456", HEADER_KEY_VALUE_DELIMITER, HEADER_DELIMITER, HEADER_KEY_VALUE_DELIMITER)
        );

        List<ValidationTask> ans = this.taskDao.getAll();

        assertEquals(Integer.parseInt(this.nodeA.getTextContent()), ans.get(0).reqMethod());
        assertEquals(nodeA.getTextContent(), ans.get(0).reqURL());
        assertEquals(this.nodeB.getTextContent().split(HEADER_DELIMITER)[0], ans.get(0).reqHeaders().get(0));
        assertEquals(this.nodeB.getTextContent().split(HEADER_DELIMITER)[1], ans.get(0).reqHeaders().get(1));
        assertEquals(Integer.parseInt(this.nodeA.getTextContent()), ans.get(0).validStatusCode());
        assertEquals(this.nodeB.getTextContent(), ans.get(0).validBody());
    }

    @Test
    void xmlAttributesThatCanBeEmptyInDatafileAreParsedOk() throws IOException, SAXException, XMLParseException {
        given(this.doc.getElementsByTagName(VALIDATION_TAG)).willReturn(this.nodes);
        given(this.xmlParser.parse(any(InputStream.class))).willReturn(this.doc);
        given(this.nodes.getLength()).willReturn(1);

        given(this.nodeB.getTextContent()).willReturn("");

        List<ValidationTask> ans = this.taskDao.getAll();

        assertThat(ans.get(0).reqHeaders().isEmpty()).isTrue();
        assertEquals("", ans.get(0).validBody());
    }

    @Test
    void whenGetAllThrowsSAXExceptionThenErrorIsLogged() throws IOException, SAXException {
        given(this.xmlParser.parse(any(InputStream.class))).willThrow(SAXException.class);
        Logger logger = mock(Logger.class);
        this.taskDao.setLogger(logger);

        assertThrows(XMLParseException.class, () -> this.taskDao.getAll());
        verify(logger).error(anyString());
    }
}
