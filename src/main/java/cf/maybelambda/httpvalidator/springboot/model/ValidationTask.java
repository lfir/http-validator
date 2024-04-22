package cf.maybelambda.httpvalidator.springboot.model;

import java.util.List;

public record ValidationTask(int reqMethod, String reqURL, List<String> reqHeaders, int validStatusCode, String validBody) {

    public boolean isValid(int statusCode, String body) {
        return this.validStatusCode == statusCode && body.contains(this.validBody);
    }
}
