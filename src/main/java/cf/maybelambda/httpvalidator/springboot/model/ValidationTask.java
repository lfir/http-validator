package cf.maybelambda.httpvalidator.springboot.model;

import java.util.List;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * Represents a validation task with HTTP request details and expected response criteria.
 *
 * @param reqMethod The HTTP request method (e.g., GET).
 * @param reqURL The URL for the HTTP request.
 * @param reqHeaders The headers for the HTTP request.
 * @param validStatusCode The expected status code for a valid response.
 * @param validBody The expected substring in the response body for a valid response.
 */
public record ValidationTask(int reqMethod, String reqURL, List<String> reqHeaders, int validStatusCode, String validBody) {

    /**
     * Constructor for ValidationTask.
     * <p>
     * Ensures that none of the parameters reqURL, reqHeaders, or validBody are null.
     *
     * @param reqMethod The HTTP request method.
     * @param reqURL The URL for the HTTP request. Must not be null.
     * @param reqHeaders The headers for the HTTP request. Must not be null.
     * @param validStatusCode The expected status code for a valid response.
     * @param validBody The expected substring in the response body for a valid response. Must not be null.
     * @throws NullPointerException if reqURL, reqHeaders, or validBody are null.
     */
    public ValidationTask {
        requireNonNull(reqURL);
        requireNonNull(reqHeaders);
        requireNonNull(validBody);
    }

    /**
     * Checks if the response status code and body match the expected criteria.
     * <p>
     * This method validates the response by comparing the status code and checking if the response body
     * contains the expected substring.
     *
     * @param statusCode The status code of the HTTP response.
     * @param body The body of the HTTP response.
     * @return True if the status code matches and the body contains the expected substring, false otherwise.
     */
    public boolean isValid(int statusCode, String body) {
        boolean res = false;
        if (nonNull(body)) {
            res = this.validStatusCode == statusCode && body.contains(this.validBody);
        }
        return res;
    }
}
