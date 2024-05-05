package cf.maybelambda.httpvalidator.springboot.model;

import org.springframework.lang.NonNull;

import java.util.List;

import static java.util.Objects.requireNonNull;


public record ValidationTask(int reqMethod, String reqURL, List<String> reqHeaders, int validStatusCode, String validBody) {
    public ValidationTask {
        requireNonNull(reqURL);
        requireNonNull(reqHeaders);
        requireNonNull(validBody);
    }

    public boolean isValid(int statusCode, @NonNull String body) {
        return this.validStatusCode == statusCode && body.contains(this.validBody);
    }
}
