package org.donghaobin.service;

import org.donghaobin.entity.UserAccount;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class CurrentUserService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    public CurrentUserService(AuthService authService) {
        this.authService = authService;
    }

    public UserAccount requireUser(HttpServletRequest request) {
        return authService.authenticate(extractToken(request));
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
