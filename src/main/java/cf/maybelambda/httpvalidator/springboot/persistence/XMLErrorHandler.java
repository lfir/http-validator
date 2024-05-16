package cf.maybelambda.httpvalidator.springboot.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

public class XMLErrorHandler implements ErrorHandler {
    private static Logger logger = LoggerFactory.getLogger(XMLErrorHandler.class);
    private static final String logmsg = "Warning during Schema Validation of the datafile";

    @Override
    public void warning(SAXParseException e) {
        logger.warn(logmsg, e);
    }

    @Override
    public void error(SAXParseException e) throws SAXParseException {
        throw e;
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXParseException {
        throw e;
    }

    void setLogger(Logger logger) { XMLErrorHandler.logger = logger; }
}
