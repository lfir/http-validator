package cf.maybelambda.httpvalidator.springboot.model;

import java.util.List;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;


public record ValidationTask(int reqMethod, String reqURL, List<String> reqHeaders, int validStatusCode, String validBody) {
    public ValidationTask {
        requireNonNull(reqURL);
        requireNonNull(reqHeaders);
        requireNonNull(validBody);
    }

    public boolean isValid(int statusCode, String body) {
        boolean res = false;
        if (nonNull(body)) {
            res = this.validStatusCode == statusCode && body.contains(this.validBody);
        }
        return res;
    }
}
