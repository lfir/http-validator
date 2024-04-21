package cf.maybelambda.httpvalidator.springboot.model;

public record ValidationTask(int reqMethod, String reqURL, String reqBody, int validStatusCode, String validBody) {

    public boolean isValid(int statusCode, String body) {
        return this.validStatusCode == statusCode && body.contains(this.validBody);
    }
}
