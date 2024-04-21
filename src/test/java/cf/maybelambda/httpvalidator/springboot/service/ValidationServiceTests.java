package cf.maybelambda.httpvalidator.springboot.service;

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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
public class ValidationServiceTests {
    @Autowired
    private ValidationService vs;

    @Test
    void xmlWithNoValidationTagsProducesZeroTasks() throws IOException, SAXException {
        DocumentBuilder db = mock(DocumentBuilder.class);
        Document doc = mock(Document.class);
        NodeList nodes = mock(NodeList.class);
        given(db.parse(any(File.class))).willReturn(doc);
        given(doc.getElementsByTagName(anyString())).willReturn(nodes);
        given(nodes.getLength()).willReturn(0);
        this.vs.setXmlParser(db);

        this.vs.readTasksFromDataFile();

        assert this.vs.getTasks().isEmpty();
    }

    @Test
    void taskDataIsReadWhenWellFormedXMLParsedWithoutErrors() throws IOException, SAXException {
        DocumentBuilder db = mock(DocumentBuilder.class);
        Document doc = mock(Document.class);
        given(db.parse(any(File.class))).willReturn(doc);
        NodeList nodes = mock(NodeList.class);
        given(doc.getElementsByTagName("validation")).willReturn(nodes);
        given(nodes.getLength()).willReturn(1);
        Node validationNode = mock(Node.class);
        given(nodes.item(anyInt())).willReturn(validationNode);
        NamedNodeMap nm = mock(NamedNodeMap.class);
        given(validationNode.getAttributes()).willReturn(nm);

        Node attributesNode = mock(Node.class);
        given(attributesNode.getTextContent()).willReturn("0");
        given(nm.getNamedItem("reqmethod")).willReturn(attributesNode);
        given(nm.getNamedItem("requrl")).willReturn(attributesNode);
        given(nm.getNamedItem("reqbody")).willReturn(attributesNode);
        given(nm.getNamedItem("ressc")).willReturn(attributesNode);
        given(nm.getNamedItem("resbody")).willReturn(attributesNode);

        this.vs.setXmlParser(db);

        this.vs.readTasksFromDataFile();

        assertEquals(Integer.parseInt(attributesNode.getTextContent()), this.vs.getTasks().get(0).reqMethod());
        assertEquals(attributesNode.getTextContent(), this.vs.getTasks().get(0).reqURL());
        assertEquals(attributesNode.getTextContent(), this.vs.getTasks().get(0).reqBody());
        assertEquals(Integer.parseInt(attributesNode.getTextContent()), this.vs.getTasks().get(0).validStatusCode());
        assertEquals(attributesNode.getTextContent(), this.vs.getTasks().get(0).validBody());
    }

    @Test
    void execValidationsSendsRequestViaHTTPClient() throws IOException, InterruptedException, SAXException {
        DocumentBuilder db = mock(DocumentBuilder.class);
        Document doc = mock(Document.class);
        given(db.parse(any(File.class))).willReturn(doc);
        NodeList nodes = mock(NodeList.class);
        given(doc.getElementsByTagName(anyString())).willReturn(nodes);
        given(nodes.getLength()).willReturn(1);

        Node n0 = mock(Node.class);
        Node n1 = mock(Node.class);
        given(nodes.item(anyInt())).willReturn(n0);
        NamedNodeMap nm = mock(NamedNodeMap.class);
        given(n0.getAttributes()).willReturn(nm);
        given(n0.getTextContent()).willReturn("0");
        given(n1.getTextContent()).willReturn("http://localhost");
        given(nm.getNamedItem("reqmethod")).willReturn(n0);
        given(nm.getNamedItem("requrl")).willReturn(n1);
        given(nm.getNamedItem("reqbody")).willReturn(n0);
        given(nm.getNamedItem("ressc")).willReturn(n0);
        given(nm.getNamedItem("resbody")).willReturn(n0);

        HttpClient cl = mock(HttpClient.class);
        given(cl.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).willReturn(mock(HttpResponse.class));

        this.vs.setClient(cl);
        this.vs.setXmlParser(db);

        this.vs.execValidations();

        assertEquals(Integer.parseInt(n0.getTextContent()), this.vs.getTasks().get(0).reqMethod());
        assertEquals(n1.getTextContent(), this.vs.getTasks().get(0).reqURL());
        verify(cl).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}
