package com.example.urikkiriserver.global.security.jwt;

import com.example.urikkiriserver.domain.user.presentation.dto.response.TokenResponse;
import com.example.urikkiriserver.global.exception.ExpiredJwt;
import com.example.urikkiriserver.global.exception.InvalidJwt;
import com.example.urikkiriserver.global.security.auth.AuthDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.Date;

@Component
public class JwtProvider {

    private final JwtProperties jwtProperties;
    private final AuthDetailsService authDetailsService;
    private final SecretKeySpec secretKeySpec;

    private static final String ACCESS_TOKEN = "access";

    public JwtProvider(JwtProperties jwtProperties, AuthDetailsService authDetailsService) {
        this.jwtProperties = jwtProperties;
        this.authDetailsService = authDetailsService;
        this.secretKeySpec = new SecretKeySpec(jwtProperties.secretKey().getBytes(), SignatureAlgorithm.HS256.getJcaName());
    }

    public String generateToken(String accountId, String type, Long exp) {
        return Jwts.builder()
            .signWith(secretKeySpec)
            .setSubject(accountId)
            .setHeaderParam("type", type)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + exp * 1000))
            .compact();
    }

    private String generateAccessToken(String accountId) {
        return generateToken(accountId, ACCESS_TOKEN, jwtProperties.accessExp());
    }

    public TokenResponse createToken(String accountId) {
        return TokenResponse.of(
            generateAccessToken(accountId),
            LocalDateTime.now().plusSeconds(jwtProperties.accessExp())
        );
    }

    public String parseToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith(jwtProperties.prefix())) {
            return bearerToken.replace(jwtProperties.prefix(), "").trim();
        }
        return null;
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtProperties.header());
        return parseToken(bearerToken);
    }

    private Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(secretKeySpec)
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (Exception e) {
            if (e instanceof io.jsonwebtoken.ExpiredJwtException) {
                throw ExpiredJwt.EXCEPTION;
            } else {
                throw InvalidJwt.EXCEPTION;
            }
        }
    }

    private String getTokenSubject(String token) {
        return getClaims(token).getSubject();
    }

    public Date getExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    public Authentication authentication(String token) {
        UserDetails userDetails = authDetailsService.loadUserByUsername(getTokenSubject(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }
}
