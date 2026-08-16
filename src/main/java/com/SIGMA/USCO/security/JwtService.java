package com.SIGMA.USCO.security;

import com.SIGMA.USCO.common.exception.InternalException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.expiration:18000000}")
    private long expirationMillis;

    private Key getSignInKey(){
        byte[] KeyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(KeyBytes);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }


    public String generateToken(Map<String,Object> extraClaims, UserDetails userDetails) {
        // ponytail: claim 'role' único para el frontend; multi-rol elige el primero (el frontend asume rol único)
        Map<String,Object> claims = new HashMap<>(extraClaims);
        userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .ifPresent(role -> claims.put("role", role));

        return Jwts.builder()

                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims getAllClaims(String token){
        try {
            return Jwts
                    .parser()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            throw new InternalException("Invalid or expired JWT token", e);
        }
    }


    private <T> T getClaim(String token, Function<Claims,T> claimsT){
        Claims claims = getAllClaims(token);
        return claimsT.apply(claims);

    }

    public String getUsername(String token){
        return getClaim(token,Claims::getSubject);
    }

    public Date getExpirationDate (String token){
        return getClaim(token,Claims::getExpiration);
    }

    private boolean tokenExpired (String token){
        return  getClaim(token,Claims::getExpiration).before(new Date());
    }

    public  boolean validateToken (String token, UserDetails userDetails){
        final String username = getUsername(token);

        return (username.equals(userDetails.getUsername()) && !tokenExpired(token));
    }

}
