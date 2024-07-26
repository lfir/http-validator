package cf.maybelambda.httpvalidator.springboot.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

/**
 * Custom error handler for XML schema validation errors.
 */
public class XMLErrorHandler implements ErrorHandler {
    private static Logger logger = LoggerFactory.getLogger(XMLErrorHandler.class);
    private static final String logmsg = "Warning during Schema Validation of the datafile";

    /**
     * Handles warnings encountered during XML schema validation.
     * The exception is logged but not thrown again.
     *
     * @param e The warning exception.
     */
    @Override
    public void warning(SAXParseException e) {
        logger.warn(logmsg, e);
    }

    /**
     * Handles errors encountered during XML schema validation.
     * This method rethrows the exception to indicate a validation error.
     *
     * @param e The error exception.
     * @throws SAXParseException Always thrown to indicate a validation error.
     */
    @Override
    public void error(SAXParseException e) throws SAXParseException {
        throw e;
    }

    /**
     * Handles fatal errors encountered during XML schema validation.
     * This method rethrows the exception to indicate a critical validation error.
     *
     * @param e The fatal error exception.
     * @throws SAXParseException Always thrown to indicate a critical validation error.
     */
    @Override
    public void fatalError(SAXParseException e) throws SAXParseException {
        throw e;
    }

    /**
     * Sets a custom logger for the XMLErrorHandler class. Used for tests.
     *
     * @param logger The logger to be used.
     */
    void setLogger(Logger logger) { XMLErrorHandler.logger = logger; }
}
