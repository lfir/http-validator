package cf.maybelambda.httpvalidator.springboot.filter;

import cf.maybelambda.httpvalidator.springboot.service.JwtAuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to intercept incoming requests and validate JWT tokens in the Authorization header.
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    public static final String AUTHORIZATION_HEADER_KEY = "Authorization";

    @Autowired
    private JwtAuthenticationService authServ;

    /**
     * Filters incoming HTTP requests. If the JWT token in the Authorization header is valid, proceed with the filter chain.
     * If token is invalid, send back unauthorized status response.
     *
     * @param request  HTTP servlet request
     * @param response HTTP servlet response
     * @param chain    Filter chain to proceed with the request
     * @throws ServletException If a servlet-specific error occurs
     * @throws IOException      If an I/O error occurs during filtering
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        if (!authServ.isValidToken(request.getHeader(AUTHORIZATION_HEADER_KEY))) {
            response.reset();
            response.setStatus(401);
            response.getOutputStream().flush();
            response.getOutputStream().close();
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * Sets the authentication service used for validating JWT tokens.
     *
     * @param authenticationService JwtAuthenticationService instance
     */
    void setAuthenticationService(JwtAuthenticationService authenticationService) {
        this.authServ = authenticationService;
    }
}
