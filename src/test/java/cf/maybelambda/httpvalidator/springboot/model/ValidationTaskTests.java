package cf.maybelambda.httpvalidator.springboot.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static cf.maybelambda.httpvalidator.springboot.service.ValidationService.HEADER_KEY_VALUE_DELIMITER;
import static java.util.Collections.emptyList;
import static javax.swing.text.html.FormSubmitEvent.MethodType.GET;
import static javax.swing.text.html.FormSubmitEvent.MethodType.POST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ValidationTaskTests {
    private static final String header = "Accept" + HEADER_KEY_VALUE_DELIMITER + "*/*";
    private final JsonNode reqBody = mock(JsonNode.class);

    @Test
    void isValidReturnsTrueWhenExpectedDataMatchesReceivedOneAndFalseOtherwise() {
        ValidationTask vt = new ValidationTask(
            GET, "http://localhost", emptyList(), this.reqBody,200, "test body"
        );

        // Invalid because of different status codes
        assertThat(vt.isValid(400, "test body")).isFalse();
        // Not valid due to expected body not present in the body of the response
        assertThat(vt.isValid(200, "other")).isFalse();
        assertThat(vt.isValid(200, null)).isFalse();
        assertThat(vt.isValid(200, "a longer test body string")).isTrue();
    }

    @Test
    void equalsReturnsTrueWhenSameObject() {
        ValidationTask task = new ValidationTask(
            POST, "http://ex.com", List.of(header), this.reqBody,200, "X"
        );

        assertThat(task.equals(task)).isTrue();
    }

    @Test
    void equalsReturnsFalseWhenDifferentType() {
        ValidationTask task = new ValidationTask(
            POST, "http://ex.com", List.of(header), this.reqBody,200, "X"
        );
        String other = "Not a ValidationTask";

        assertThat(task.equals(other)).isFalse();
    }

    @Test
    void equalsReturnsFalseWhenDifferentReqMethod() {
        ValidationTask task1 = new ValidationTask(
            POST, "http://ex.com", List.of(header), this.reqBody,200, "X"
        );
        ValidationTask task2 = new ValidationTask(
            GET, "http://ex.com", List.of(header), this.reqBody,200, "X"
        );

        assertThat(task1.equals(task2)).isFalse();
    }

    @Test
    void equalsReturnsFalseWhenDifferentReqURL() {
        ValidationTask task1 = new ValidationTask(
            POST, "http://ex.com", emptyList(), this.reqBody,200, "X"
        );
        ValidationTask task2 = new ValidationTask(
            POST, "http://example.com", emptyList(), this.reqBody, 200, "X"
        );

        assertThat(task1.equals(task2)).isFalse();
    }

    @Test
    void equalsReturnsFalseWhenDifferentReqHeaders() {
        ValidationTask task1 = new ValidationTask(
            POST, "http://ex.com", emptyList(), this.reqBody,200, "X"
        );
        ValidationTask task2 = new ValidationTask(
            POST, "http://ex.com", List.of(header), this.reqBody,200, "X"
        );

        assertThat(task1.equals(task2)).isFalse();
    }

    @Test
    void equalsReturnsFalseWhenDifferentValidStatusCode() {
        ValidationTask task1 = new ValidationTask(
            POST, "http://ex.com", List.of(header), this.reqBody,200, "X"
        );
        ValidationTask task2 = new ValidationTask(
            POST, "http://ex.com", List.of(header), this.reqBody,404, "X"
        );

        assertThat(task1.equals(task2)).isFalse();
    }

    @Test
    void equalsReturnsFalseWhenDifferentValidBody() {
        ValidationTask task1 = new ValidationTask(
            POST, "http://ex.com", List.of(header), this.reqBody,200, "X"
        );
        ValidationTask task2 = new ValidationTask(
            POST, "http://ex.com", List.of(header), this.reqBody,200, "O"
        );

        assertThat(task1.equals(task2)).isFalse();
    }

    @Test
    void equalsReturnsTrueWhenSameFieldValues() {
        ValidationTask task1 = new ValidationTask(
            POST, "http://ex.com", List.of(header), this.reqBody,200, "X"
        );
        ValidationTask task2 = new ValidationTask(
            POST, "http://ex.com", List.of(header), this.reqBody,200, "X"
        );

        assertThat(task1.equals(task2)).isTrue();
    }

    @Test
    void sameHashCodeWhenEqualObjects() {
        ValidationTask task1 = new ValidationTask(
            POST, "http://ex.com", List.of(header), this.reqBody,200, "X"
        );
        ValidationTask task2 = new ValidationTask(
            POST, "http://ex.com", List.of(header), this.reqBody,200, "X"
        );

        assertThat(task1.hashCode()).isEqualTo(task2.hashCode());
    }
}
