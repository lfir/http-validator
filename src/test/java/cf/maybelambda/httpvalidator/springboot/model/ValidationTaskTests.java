package cf.maybelambda.httpvalidator.springboot.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidationTaskTests {
    @Test
    void isValidReturnsTrueWhenExpectedDataMatchesReceivedOneAndFalseOtherwise() {
        ValidationTask vt = new ValidationTask(0, "http://localhost", Collections.emptyList(), 200, "test body");

        // Invalid because of different status codes
        assertThat(vt.isValid(400, "test body")).isFalse();
        // Not valid due to expected body not present in the body of the response
        assertThat(vt.isValid(200, "other")).isFalse();
        assertThat(vt.isValid(200, "a longer test body string")).isTrue();
    }

    @Test
    void equalsReturnsTrueWhenSameObject() {
        ValidationTask task = new ValidationTask(1, "http://example.com", List.of("Header1"), 200, "Body");

        assertThat(task.equals(task)).isTrue();
    }

    @Test
    void equalsReturnsFalseWhenDifferentType() {
        ValidationTask task = new ValidationTask(1, "http://example.com", List.of("Header1"), 200, "Body");
        String other = "Not a ValidationTask";

        assertThat(task.equals(other)).isFalse();
    }

    @Test
    void equalsReturnsFalseWhenDifferentReqMethod() {
        ValidationTask task1 = new ValidationTask(1, "http://example.com", List.of("Header1"), 200, "Body");
        ValidationTask task2 = new ValidationTask(2, "http://example.com", List.of("Header1"), 200, "Body");

        assertThat(task1.equals(task2)).isFalse();
    }

    @Test
    void equalsReturnsFalseWhenDifferentReqURL() {
        ValidationTask task1 = new ValidationTask(1, "http://example.com", List.of("Header1"), 200, "Body");
        ValidationTask task2 = new ValidationTask(1, "http://example2.com", List.of("Header1"), 200, "Body");

        assertThat(task1.equals(task2)).isFalse();
    }

    @Test
    void equalsReturnsFalseWhenDifferentReqHeaders() {
        ValidationTask task1 = new ValidationTask(1, "http://example.com", List.of("Header1"), 200, "Body");
        ValidationTask task2 = new ValidationTask(1, "http://example.com", List.of("Header2"), 200, "Body");

        assertThat(task1.equals(task2)).isFalse();
    }

    @Test
    void equalsReturnsFalseWhenDifferentValidStatusCode() {
        ValidationTask task1 = new ValidationTask(1, "http://example.com", List.of("Header1"), 200, "Body");
        ValidationTask task2 = new ValidationTask(1, "http://example.com", List.of("Header1"), 404, "Body");

        assertThat(task1.equals(task2)).isFalse();
    }

    @Test
    void equalsReturnsFalseWhenDifferentValidBody() {
        ValidationTask task1 = new ValidationTask(1, "http://example.com", List.of("Header1"), 200, "Body");
        ValidationTask task2 = new ValidationTask(1, "http://example.com", List.of("Header1"), 200, "Different Body");

        assertThat(task1.equals(task2)).isFalse();
    }

    @Test
    void equalsReturnsTrueWhenSameFieldValues() {
        ValidationTask task1 = new ValidationTask(1, "http://example.com", List.of("Header1"), 200, "Body");
        ValidationTask task2 = new ValidationTask(1, "http://example.com", List.of("Header1"), 200, "Body");

        assertThat(task1.equals(task2)).isTrue();
    }

    @Test
    void sameHashCodeWhenEqualObjects() {
        ValidationTask task1 = new ValidationTask(1, "http://example.com", List.of("Header1"), 200, "Body");
        ValidationTask task2 = new ValidationTask(1, "http://example.com", List.of("Header1"), 200, "Body");

        assertThat(task1.hashCode()).isEqualTo(task2.hashCode());
    }
}
