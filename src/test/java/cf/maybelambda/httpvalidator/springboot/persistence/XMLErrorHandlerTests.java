package cf.maybelambda.httpvalidator.springboot.persistence;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.xml.sax.SAXParseException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class XMLErrorHandlerTests {
    private final XMLErrorHandler handler = new XMLErrorHandler();
    private final SAXParseException exception = mock(SAXParseException.class);

    @Test
    void validationWarningIsLogged() {
        Logger logger = mock(Logger.class);
        this.handler.setLogger(logger);

        this.handler.warning(this.exception);

        verify(logger).warn(anyString(), any(SAXParseException.class));
    }

    @Test
    void validationErrorOrFatalErrorThrowSAXParseException() {
        assertThrows(SAXParseException.class, () -> this.handler.error(this.exception));
        assertThrows(SAXParseException.class, () -> this.handler.fatalError(this.exception));
    }
}
