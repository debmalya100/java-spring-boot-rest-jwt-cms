package com.knowledge.api.security;

import com.knowledge.api.model.UserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("Starting JWT filter processing");
        String token = extractToken(request);

        if (token != null) {
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                log.debug("Token claims: {}", claims);

                // Extract the nested userdetail object
                LinkedHashMap<String, Object> userdetail = (LinkedHashMap<String, Object>) claims.get("userdetail");
                if (userdetail != null) {
                    log.debug("Found userdetail: {}", userdetail);

                    // Extract user_master_id and client_ids from the nested structure
                    Object userMasterId = userdetail.get("user_master_id");
                    Object clientIds = userdetail.get("client_ids");

                    log.debug("Extracted user_master_id: {}, client_ids: {}", userMasterId, clientIds);

                    UserDetails userDetails = new UserDetails();
                    userDetails.setUserMasterId(userMasterId != null ? (Integer)userMasterId : null);
                    userDetails.setClientIds(clientIds != null ? String.valueOf(clientIds) : null);

                    log.debug("Created UserDetails: {}", userDetails);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Set authentication in SecurityContext");
                } else {
                    log.warn("No userdetail object found in token");
                }
            } catch (Exception e) {
                log.error("Error processing JWT token: ", e);
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/") ||
                path.startsWith("/api/v1/auth/") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs/");
    }
}