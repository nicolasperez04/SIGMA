package com.SIGMA.USCO.security;

import com.SIGMA.USCO.common.web.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 5;
    private static final long WINDOW_MILLIS = 60_000L;
    private static final long CLEANUP_EVERY_INSERTS = 100L;
    private static final long STALE_THRESHOLD_MILLIS = 120_000L;

    private final ObjectMapper objectMapper;
    private final AtomicLong requestsSeen = new AtomicLong();

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean enabled;

    // ponytail: token bucket por IP en memoria {count, windowStartMillis}; ventana fija 60s,
    // aproximada entre nodos y tras reinicios — sirve para auth endpoints locales.
    private final ConcurrentHashMap<String, long[]> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getServletPath();
        return !(path.startsWith("/auth/login")
                || path.startsWith("/auth/register")
                || path.startsWith("/auth/forgot-password"));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        long now = System.currentTimeMillis();

        long[] bucket = buckets.compute(ip, (key, existing) -> {
            if (existing == null || now - existing[1] > WINDOW_MILLIS) {
                return new long[]{0, now};
            }
            return existing;
        });

        if (bucket[0] >= MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.error("Demasiados intentos. Intente de nuevo en un minuto.")));
            return;
        }

        bucket[0]++;

        // ponytail: barrido ocasional de entradas viejas (cada 100 requests) — evita fuga de memoria
        // sin costo por request; un cache expirable (Caffeine) sería el upgrade si el tráfico creciera.
        if (requestsSeen.incrementAndGet() % CLEANUP_EVERY_INSERTS == 0) {
            long cutoff = now - STALE_THRESHOLD_MILLIS;
            buckets.entrySet().removeIf(entry -> entry.getValue()[1] < cutoff);
        }

        chain.doFilter(request, response);
    }
}