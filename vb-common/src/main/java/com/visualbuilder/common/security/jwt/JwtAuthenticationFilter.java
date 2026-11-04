package com.visualbuilder.common.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visualbuilder.common.core.constant.RedisKeyConst;
import com.visualbuilder.common.core.exception.ErrorCode;
import com.visualbuilder.common.core.result.R;
import com.visualbuilder.common.security.utils.SecurityUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @SuppressWarnings("unchecked")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
            Claims claims = jwtUtils.parseToken(token);
            String jti = claims.getId();
            Long userId = Long.parseLong(claims.getSubject());

            // 一次性批量查询黑名单与禁用标记（MGET，避免多次 Redis 往返）
            List<String> cacheKeys = List.of(
                    RedisKeyConst.tokenBlacklist(jti),
                    RedisKeyConst.userDisabled(userId));
            List<String> cacheValues = redisTemplate.opsForValue().multiGet(cacheKeys);

            // 检查 token 黑名单（logout 后 token 被加入黑名单）
            if (cacheValues != null && cacheValues.size() > 0 && cacheValues.get(0) != null) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                        ErrorCode.UNAUTHORIZED, "Token已失效，请重新登录");
                return;
            }

            // 检查用户是否被禁用（管理员操作后即时生效）
            if (cacheValues != null && cacheValues.size() > 1 && cacheValues.get(1) != null) {
                log.warn("用户已被禁用，拒绝访问: userId={}", userId);
                writeError(response, HttpServletResponse.SC_FORBIDDEN,
                        ErrorCode.ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
                return;
            }

            String username = claims.get("username", String.class);

            // 从 JWT claims 提取角色和权限
            List<String> roles = Collections.emptyList();
            List<String> permissions = Collections.emptyList();
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();

            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List<?> rawList) {
                roles = rawList.stream().map(Object::toString).toList();
                roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
            }

            Object permsObj = claims.get("permissions");
            if (permsObj instanceof List<?> rawList) {
                permissions = rawList.stream().map(Object::toString).toList();
                permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
            }

            // 将用户信息存入 SecurityContext（含角色/权限 authorities）
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 存入 ThreadLocal 方便业务层获取
            SecurityUtils.setContext(userId, username, roles, permissions);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityUtils.clearContext();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(TOKEN_PREFIX)) {
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    /**
     * 以统一响应体（{@link R}）输出错误，保证与全局异常处理格式一致。
     */
    private void writeError(HttpServletResponse response, int httpStatus, int code, String message)
            throws IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(code, message)));
    }
}
