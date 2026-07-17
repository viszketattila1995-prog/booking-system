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
 * Az AI asszisztens minden hívása egy fizetős, külső API-hívást indít - a
 * puszta JWT-s auth (bárki bejelentkezett user hívhatja) önmagában nem
 * korlátozza a költséget egy hibás kliens vagy visszaélés esetén. Ugyanaz a
 * fixed-window minta, mint a LoginRateLimitFilter-nél (lásd ott a részletes
 * indoklást), csak lazább limittel és más útvonalon.
 */
public class AssistantRateLimitFilter extends OncePerRequestFilter {

    private static final String CHAT_PATH = "/api/assistant/chat";
    private static final int MAX_REQUESTS_PER_WINDOW = 15;
    private static final long WINDOW_MS = 60_000;

    private final ConcurrentHashMap<String, Window> requestsByIp = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !CHAT_PATH.equals(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        Window window = requestsByIp.compute(request.getRemoteAddr(), (ip, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new Window(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (window.count.get() > MAX_REQUESTS_PER_WINDOW) {
            long retryAfterSeconds = Math.max(1, (WINDOW_MS - (System.currentTimeMillis() - window.windowStart)) / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many assistant requests, try again later\"}");
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
