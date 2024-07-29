package cf.maybelambda.httpvalidator.springboot.util;

import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static cf.maybelambda.httpvalidator.springboot.util.HttpSendOutcomeWrapper.NET_ERR_MSG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class HttpSendOutcomeWrapperTests {
    private final HttpResponse<String> httpResponse = mock(HttpResponse.class);

    @Test
    void isWholeResponseReturnsTrueWhenHttpResponseAndItsBodyAreAvailable() {
        HttpSendOutcomeWrapper wrapper = new HttpSendOutcomeWrapper(this.httpResponse);
        given(this.httpResponse.body()).willReturn("");

        assertThat(wrapper.isWholeResponse()).isTrue();
    }

    @Test
    void isWholeResponseReturnsFalseWhenHttpResponseBodyIsNull() {
        HttpSendOutcomeWrapper wrapper = new HttpSendOutcomeWrapper(this.httpResponse);
        given(this.httpResponse.body()).willReturn(null);

        assertThat(wrapper.isWholeResponse()).isFalse();
    }

    @Test
    void isWholeResponseReturnsFalseWhenHttpResponseIsNull() {
        HttpSendOutcomeWrapper wrapper = new HttpSendOutcomeWrapper(this.httpResponse);
        wrapper.setResponse(null);

        assertThat(wrapper.isWholeResponse()).isFalse();
    }

    @Test
    void getStatusCodeReturnsTheStatusCodeOfTheHttpResponseWhenExceptionFieldIsNull() {
        HttpSendOutcomeWrapper wrapper = new HttpSendOutcomeWrapper(this.httpResponse);
        given(this.httpResponse.statusCode()).willReturn(200);

        assertThat(wrapper.getStatusCode()).isEqualTo(200);
    }

    @Test
    void getStatusCodeReturnsMinusOneWhenExceptionFieldIsNotNull() {
        HttpSendOutcomeWrapper wrapper = new HttpSendOutcomeWrapper(new ArithmeticException());

        assertThat(wrapper.getStatusCode()).isEqualTo(-1);
    }

    @Test
    void getBodyReturnsTheBodyOfTheHttpResponseWhenExceptionFieldIsNull() {
        HttpSendOutcomeWrapper wrapper = new HttpSendOutcomeWrapper(this.httpResponse);
        given(this.httpResponse.body()).willReturn("");

        assertThat(wrapper.getBody()).isEmpty();
    }

    @Test
    void getBodyReturnsNetworkErrorMessageWhenExceptionFieldIsNotNull() {
        HttpSendOutcomeWrapper wrapper = new HttpSendOutcomeWrapper(new NumberFormatException());

        assertThat(wrapper.getBody()).isEqualTo(NET_ERR_MSG);
    }
}
