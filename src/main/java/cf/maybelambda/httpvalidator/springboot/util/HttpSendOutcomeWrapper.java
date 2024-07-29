package cf.maybelambda.httpvalidator.springboot.util;

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
     * Message about a network error that occurred while attempting a request.
     */
    static final String NET_ERR_MSG = "Network error. A connection to the server could not be established (unreachable / refused / timed out).";

    private HttpResponse<String> res;
    private Throwable ex;

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
        return isNull(this.ex) ? this.res.statusCode() : -1;
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
