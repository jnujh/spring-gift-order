package gift.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 토큰 추출
        String token = extractToken(request);

        // 2. 토큰이 있는 경우 -> 유효성 검사 및 파싱 + memberId 추출
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtTokenProvider.validateAndParseClaims(token);
                Long memberId = jwtTokenProvider.getMemberId(claims);
                request.setAttribute("memberId", memberId);
            } catch (ExpiredJwtException e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "만료된 토큰입니다.");
                return;
            } catch (UnsupportedJwtException e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "지원되지 않는 토큰 형식입니다.");
                return;
            } catch (MalformedJwtException e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "손상된 토큰입니다.");
                return;
            } catch (SignatureException e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "토큰 서명이 유효하지 않습니다.");
                return;
            } catch (IllegalArgumentException e) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "토큰이 비어있거나 잘못되었습니다.");
                return;
            }
        }

        // 3. 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    // Authorization 헤더에서 토큰 추출
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    // 인증이 필요없는 경로는 필터 skip
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // 인증이 필요없는 경로들
        return path.startsWith("/api/members/register") ||
                path.startsWith("/api/members/login") ||
                path.equals("/");
    }
}