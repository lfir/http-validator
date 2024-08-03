package cf.maybelambda.httpvalidator.springboot.model;

import com.fasterxml.jackson.databind.JsonNode;

import javax.swing.text.html.FormSubmitEvent.MethodType;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

/**
 * Represents a validation task with HTTP request details and expected response criteria.
 */
public record ValidationTask(MethodType reqMethod, String reqURL, List<String> reqHeaders, JsonNode reqBody, int validStatusCode, String validBody) {

    /**
     * Constructor for ValidationTask. Ensures that none of the parameters are null.
     *
     * @param reqMethod The HTTP request method, GET or POST.
     * @param reqURL The URL for the HTTP request.
     * @param reqHeaders The headers for the HTTP request.
     * @param reqBody The JSON body for a POST request.
     * @param validStatusCode The expected status code for a valid response.
     * @param validBody The expected substring in the response body for a valid response.
     * @throws NullPointerException if any argument is null.
     */
    public ValidationTask {
        requireNonNull(reqMethod);
        requireNonNull(reqURL);
        requireNonNull(reqHeaders);
        requireNonNull(reqBody);
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
     * @return {@code true} if the status code matches and the body contains the expected substring, {@code false} otherwise.
     */
    public boolean isValid(int statusCode, String body) {
        boolean res = false;
        if (nonNull(body)) {
            res = this.validStatusCode == statusCode && body.contains(this.validBody);
        }
        return res;
    }

    /**
     * Compares this ValidationTask to the specified object for equality.
     * <p>
     * The comparison is based on the request method, request URL, request headers, request body,
     * valid status code, and valid response body.
     *
     * @param o The object to compare with this ValidationTask.
     * @return {@code true} if the specified object is equal to this ValidationTask; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ValidationTask that)) return false;
        return this.reqMethod == that.reqMethod &&
            this.validStatusCode == that.validStatusCode &&
            Objects.equals(this.reqURL, that.reqURL) &&
            Objects.equals(this.validBody, that.validBody) &&
            Objects.equals(this.reqHeaders, that.reqHeaders) &&
            Objects.equals(this.reqBody, that.reqBody);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reqMethod, reqURL, reqHeaders, reqBody, validStatusCode, validBody);
    }
}
