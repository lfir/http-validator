package cf.maybelambda.httpvalidator.springboot.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import javax.management.modelmbean.XMLParseException;

/**
 * Custom error handler for XML schema validation errors. Also contains utility methods related to error handling.
 */
public class XMLErrorHandler implements ErrorHandler {
    private static Logger logger = LoggerFactory.getLogger(XMLErrorHandler.class);
    private static final String logmsg = "Warning during Schema Validation of the datafile";

    /**
     * Functional interface representing a function that accepts one argument and produces a result,
     * potentially throwing an exception during the process.
     *
     * <p>This interface is similar to {@link java.util.function.Function}, but it allows for checked
     * exceptions to be thrown during the function application.
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     */
    @FunctionalInterface
    interface ThrowingFunction<T, R> {
        R apply(T arg) throws Exception;
    }

    /**
     * A utility method to apply a function that may throw an exception, with additional exception handling.
     *
     * <p>This method attempts to apply the provided function to the given argument. If an exception occurs,
     * it logs the error message along with the exception and then throws an {@code XMLParseException} with
     * the same data.
     *
     * @param <T>    the type of the input to the function
     * @param <R>    the type of the result of the function
     * @param f      the function to be applied, which may throw an exception
     * @param arg    the argument to be passed to the function
     * @param logger the logger to be used
     * @param msg    the error message to be logged and included in the thrown {@code XMLParseException}
     * @return the result of the function application
     * @throws XMLParseException if the function throws any exception during its application
     *
     *  @see ThrowingFunction
     */
    static <T, R> R parseInputOrThrow(ThrowingFunction<T, R> f, T arg, Logger logger, String msg) throws XMLParseException {
        try {
            return f.apply(arg);
        } catch (Exception e) {
            logger.error(msg, e);
            throw new XMLParseException(e, msg + "\n");
        }
    }

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
