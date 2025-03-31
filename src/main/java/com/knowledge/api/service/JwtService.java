package com.knowledge.api.service;

import com.knowledge.api.model.UserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ExpiredJwtException;

import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    public Claims validateToken(String token) {
        try {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // System.out.println("Token: " + token);
            // System.out.println("Secret Key: " + secretKey);

            byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);

            // Create a SecretKey object
            SecretKey key = Keys.hmacShaKeyFor(keyBytes);

            // Parse the token
            Claims claims = Jwts.parser()
                    .setSigningKey(key)
                    .parseClaimsJws(token)
                    .getBody();

            // System.out.println("Claims: " + claims);
            return claims;
        } catch (ExpiredJwtException e) {
            System.err.println("Token expired: " + e.getMessage());
            throw new RuntimeException("Token expired", e);
        } catch (UnsupportedJwtException e) {
            System.err.println("Unsupported JWT: " + e.getMessage());
            throw new RuntimeException("Unsupported JWT", e);
        } catch (MalformedJwtException e) {
            System.err.println("Malformed JWT: " + e.getMessage());
            throw new RuntimeException("Malformed JWT", e);
        } catch (SignatureException e) {
            System.err.println("Invalid signature: " + e.getMessage());
            throw new RuntimeException("Invalid signature", e);
        } catch (Exception e) {
            System.err.println("Error parsing token: " + e.getMessage());
            throw new RuntimeException("Invalid token", e);
        }
    }

    public UserDetails extractUserDetails(String token) {

        try {
            // System.out.println("Token received: " + token);

            // Remove "Bearer " prefix and trim spaces
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7).trim();
            }

            // System.out.println("Token after trimming: " + token);

            // Validate the token and extract claims
            Claims claims = validateToken(token);
            // System.out.println("Claims extracted: " + claims);

            // Extract user details from claims
            UserDetails userDetails = new UserDetails();
            Map<String, Object> userDetailMap = claims.get("userdetail", Map.class);

            if (userDetailMap != null) {
                userDetails.setUserMasterId((Integer)userDetailMap.get("user_master_id"));
                userDetails.setClientIds(String.valueOf(userDetailMap.get("client_ids")));

            }

            // System.out.println("UserDetails extracted: " + userDetails);
            return userDetails;
        } catch (Exception e) {
            // System.err.println("Error extracting user details: " + e.getMessage());
            return null;
        }

    }
}