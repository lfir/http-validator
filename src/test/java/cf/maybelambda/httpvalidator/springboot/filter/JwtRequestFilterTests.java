package cf.maybelambda.httpvalidator.springboot.filter;

import cf.maybelambda.httpvalidator.springboot.service.JwtAuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static cf.maybelambda.httpvalidator.springboot.filter.JwtRequestFilter.AUTHORIZATION_HEADER_KEY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JwtRequestFilterTests {
    private JwtRequestFilter filter;
    private final JwtAuthenticationService authServ = mock(JwtAuthenticationService.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain chain = mock(FilterChain.class);

    @BeforeEach
    void setUp() {
        this.filter = new JwtRequestFilter();
        this.filter.setAuthenticationService(this.authServ);

        given(this.request.getHeader(AUTHORIZATION_HEADER_KEY)).willReturn("testToken");
    }

    @Test
    void doFilterInternalCallsJwtAuthenticationServiceToValidateToken() throws Exception {
        given(this.authServ.isValidToken(anyString())).willReturn(true);

        this.filter.doFilterInternal(this.request, this.response, this.chain);

        verify(this.authServ).isValidToken("testToken");
    }

    @Test
    void doFilterInternalCallsFilterChainWhenValidTokenInRequest() throws Exception {
        given(this.authServ.isValidToken(anyString())).willReturn(true);

        this.filter.doFilterInternal(this.request, this.response, this.chain);

        verify(this.chain).doFilter(this.request, this.response);
    }

    @Test
    void doFilterInternalSetsResponseStatus401WhenInvalidTokenInRequest() throws Exception {
        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        given(this.response.getOutputStream()).willReturn(outputStream);
        given(this.authServ.isValidToken(anyString())).willReturn(false);

        this.filter.doFilterInternal(this.request, this.response, this.chain);

        verify(this.response).setStatus(401);
    }
}
