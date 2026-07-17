package com.attila.bookingsystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Memóriában tartott, fixed-window rate limiter a login endpointra, kliens IP
 * szerint kulcsolva - alapvédelem jelszó brute-force ellen. Szándékosan
 * request.getRemoteAddr()-t használ, NEM az X-Forwarded-For fejlécet: az utóbbi
 * kliens által szabadon hamisítható (proxy nélküli setupban bárki tetszőleges
 * IP-t "állíthatna be" benne), ami a rate limitet triviálisan megkerülhetővé
 * tenné. Ha a szolgáltatás valaha trusted reverse proxy mögé kerül, ezt
 * server.forward-headers-strategy konfigurációval (ami felülírja a
 * getRemoteAddr()-t) kell megoldani, nem itt kézzel.
 * Egyetlen instance-ra tervezve - több instance-os skálázásnál megosztott
 * store (pl. Redis) kellene helyette.
 */
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final int MAX_ATTEMPTS_PER_WINDOW = 5;
    private static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, Window> attemptsByIp = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !LOGIN_PATH.equals(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        Window window = attemptsByIp.compute(request.getRemoteAddr(), (ip, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new Window(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (window.count.get() > MAX_ATTEMPTS_PER_WINDOW) {
            long retryAfterSeconds = Math.max(1, (WINDOW_MS - (System.currentTimeMillis() - window.windowStart)) / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many login attempts, try again later\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static final class Window {
        private final long windowStart;
        private final AtomicInteger count;

        private Window(long windowStart) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(1);
        }
    }
}
