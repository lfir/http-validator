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

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
    public static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    @Autowired
    JwtAuthenticationService authServ;

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

    void setAuthenticationService(JwtAuthenticationService authenticationService) { this.authServ = authenticationService; }
}
