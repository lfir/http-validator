package cf.maybelambda.httpvalidator.springboot.persistence;

import cf.maybelambda.httpvalidator.springboot.model.ValidationTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static cf.maybelambda.httpvalidator.springboot.persistence.XMLValidationTaskDao.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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

    @Test
    void xmlWithNoValidationTagsProducesZeroTasks() throws IOException, SAXException {
        this.taskDao.setXmlParser(this.xmlParser);
        given(this.xmlParser.parse(any(File.class))).willReturn(this.doc);
        given(doc.getElementsByTagName(anyString())).willReturn(this.nodes);
        given(this.nodes.getLength()).willReturn(0);

        assert this.taskDao.getAll().isEmpty();
    }

    @Test
    void taskDataIsReadWhenWellFormedXMLParsedWithoutErrors() throws IOException, SAXException {
        given(this.xmlParser.parse(any(File.class))).willReturn(this.doc);
        this.taskDao.setXmlParser(this.xmlParser);

        given(this.doc.getElementsByTagName(VALIDATION_TAG)).willReturn(this.nodes);
        given(this.nodes.getLength()).willReturn(1);
        given(this.nodes.item(anyInt())).willReturn(this.validationNode);
        given(validationNode.getAttributes()).willReturn(this.nm);

        given(this.nodeA.getTextContent()).willReturn("0");
        given(this.nodeB.getTextContent()).willReturn("X-H1:B32C,H2:456");
        given(this.nm.getNamedItem(REQ_METHOD_ATTR)).willReturn(this.nodeA);
        given(this.nm.getNamedItem(REQ_URL_ATTR)).willReturn(this.nodeA);
        given(this.nm.getNamedItem(REQ_HEADERS_ATTR)).willReturn(nodeB);
        given(this.nm.getNamedItem(RES_SC_ATTR)).willReturn(this.nodeA);
        given(this.nm.getNamedItem(RES_BODY_ATTR)).willReturn(this.nodeA);

        List<ValidationTask> ans = this.taskDao.getAll();

        assertEquals(Integer.parseInt(this.nodeA.getTextContent()), ans.get(0).reqMethod());
        assertEquals(nodeA.getTextContent(), ans.get(0).reqURL());
        assertEquals(this.nodeB.getTextContent().split(HEADER_DELIMITER)[0], ans.get(0).reqHeaders().get(0));
        assertEquals(this.nodeB.getTextContent().split(HEADER_DELIMITER)[1], ans.get(0).reqHeaders().get(1));
        assertEquals(Integer.parseInt(this.nodeA.getTextContent()), ans.get(0).validStatusCode());
        assertEquals(this.nodeA.getTextContent(), ans.get(0).validBody());
    }

    @Test
    void xmlAttributesThatCanBeEmptyInDatafileAreParsedOk() throws IOException, SAXException {
        given(this.xmlParser.parse(any(File.class))).willReturn(this.doc);
        this.taskDao.setXmlParser(this.xmlParser);

        given(this.doc.getElementsByTagName(VALIDATION_TAG)).willReturn(this.nodes);
        given(this.nodes.getLength()).willReturn(1);
        given(this.nodes.item(anyInt())).willReturn(this.validationNode);
        given(this.validationNode.getAttributes()).willReturn(this.nm);

        given(this.nodeA.getTextContent()).willReturn("0");
        given(this.nodeB.getTextContent()).willReturn("");
        given(this.nm.getNamedItem(REQ_METHOD_ATTR)).willReturn(this.nodeA);
        given(this.nm.getNamedItem(REQ_URL_ATTR)).willReturn(this.nodeA);
        given(this.nm.getNamedItem(REQ_HEADERS_ATTR)).willReturn(this.nodeB);
        given(this.nm.getNamedItem(RES_SC_ATTR)).willReturn(this.nodeA);
        given(this.nm.getNamedItem(RES_BODY_ATTR)).willReturn(this.nodeB);

        List<ValidationTask> ans = this.taskDao.getAll();

        assert ans.get(0).reqHeaders().isEmpty();
        assertEquals("", ans.get(0).validBody());
    }
}
