package org.donghaobin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.donghaobin.dto.AuthResponse;
import org.donghaobin.entity.AuthToken;
import org.donghaobin.entity.UserAccount;
import org.donghaobin.mapper.AuthTokenMapper;
import org.donghaobin.mapper.UserAccountMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final int TOKEN_DAYS = 7;

    private final UserAccountMapper userAccountMapper;
    private final AuthTokenMapper authTokenMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserAccountMapper userAccountMapper,
                       AuthTokenMapper authTokenMapper,
                       PasswordEncoder passwordEncoder) {
        this.userAccountMapper = userAccountMapper;
        this.authTokenMapper = authTokenMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(password);

        if (findByUsername(normalizedUsername) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "账号已存在");
        }

        UserAccount user = new UserAccount(normalizedUsername, passwordEncoder.encode(password));
        userAccountMapper.insert(user);
        return issueToken(user);
    }

    @Transactional
    public AuthResponse login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        UserAccount user = findByUsername(normalizedUsername);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        if (!passwordEncoder.matches(password == null ? "" : password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        return issueToken(user);
    }

    @Transactional(readOnly = true)
    public UserAccount authenticate(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        AuthToken authToken = authTokenMapper.selectOne(new LambdaQueryWrapper<AuthToken>()
                .eq(AuthToken::getToken, token.trim())
                .gt(AuthToken::getExpiresAt, LocalDateTime.now()));
        if (authToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        UserAccount user = userAccountMapper.selectById(authToken.getUserId());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }
        return user;
    }

    private AuthResponse issueToken(UserAccount user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        authTokenMapper.insert(new AuthToken(token, user.getId(), LocalDateTime.now().plusDays(TOKEN_DAYS)));
        return new AuthResponse(token, user);
    }

    private UserAccount findByUsername(String username) {
        return userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUsername, username)
                .last("limit 1"));
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim();
        if (normalized.length() < 3 || normalized.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号长度必须在 3 到 50 个字符之间");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码长度必须在 6 到 80 个字符之间");
        }
    }
}
