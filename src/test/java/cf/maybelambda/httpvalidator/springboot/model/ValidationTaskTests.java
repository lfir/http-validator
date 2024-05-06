package cf.maybelambda.httpvalidator.springboot.model;

import org.junit.jupiter.api.Test;

import java.util.Collections;

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
}
