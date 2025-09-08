package cf.maybelambda.httpvalidator.springboot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * A utility wrapper class for handling outcomes of HTTP requests.
 * <p>
 * This class encapsulates either a successful HTTP response or an exception
 * encountered during the request. It provides methods to query the state
 * of the outcome and to retrieve relevant details.
 */
public class HttpSendOutcomeWrapper {
    /**
     * Status code assigned to exceptional send results arising from network errors.
     */
    public static final int NET_ERR_CODE = -1;
    /**
     * Message about a network error that occurred while attempting a request.
     */
    public static final String NET_ERR_MSG = "Request was not completed: Network Error. A connection to the server "
        + "could not be established (unreachable / refused) or it timed out.";

    private HttpResponse<String> res;
    private Throwable ex;
    private static Logger logger = LoggerFactory.getLogger(HttpSendOutcomeWrapper.class);

    /**
     * Constructs an instance wrapping a successful HTTP response.
     *
     * @param res the HTTP response
     */
    public HttpSendOutcomeWrapper(HttpResponse<String> res) {
        this.res = res;
    }

    /**
     * Constructs an instance wrapping an exception encountered during the request.
     *
     * @param ex the exception
     */
    public HttpSendOutcomeWrapper(Throwable ex) {
        logger.error("HTTP EX: ", ex);
        this.ex = ex;
    }

    /**
     * Checks if the wrapper contains a complete HTTP response with a non-null body.
     *
     * @return true if the response and its body are non-null, false otherwise
     */
    public boolean isWholeResponse() {
        return nonNull(this.res) && nonNull(this.res.body());
    }

    /**
     * Gets the status code of the HTTP response.
     * <p>
     * If an exception was encountered during the request, this method returns -1.
     *
     * @return the HTTP status code or -1 if an exception occurred
     */
    public int getStatusCode() {
        return isNull(this.ex) ? this.res.statusCode() : NET_ERR_CODE;
    }

    /**
     * Gets the body of the HTTP response.
     * <p>
     * If an exception was encountered during the request, this method returns a predefined network error message.
     *
     * @return the body of the HTTP response or a network error message if an exception occurred
     */
    public String getBody() {
        return isNull(this.ex) ? this.res.body() : NET_ERR_MSG;
    }

    /**
     * Sets the HTTP response for this wrapper. Used for tests.
     *
     * @param res the HTTP response
     */
    void setResponse(HttpResponse<String> res) {
        this.res = res;
    }
}
