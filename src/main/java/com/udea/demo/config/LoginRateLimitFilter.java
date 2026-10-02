package com.udea.demo.config;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String, Ventana> ventanas = new ConcurrentHashMap<>();

    @Value("${app.auth.login-rate-limit-per-minute:30}")
    private int maxPorMinuto;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod())
                && "/api/v1/auth/login".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clave = request.getRemoteAddr();
        long minuto = Instant.now().getEpochSecond() / 60;
        Ventana ventana = ventanas.compute(clave, (k, actual) -> {
            if (actual == null || actual.minuto != minuto) return new Ventana(minuto);
            actual.intentos.incrementAndGet();
            return actual;
        });
        if (ventana.intentos.get() > maxPorMinuto) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Demasiados intentos de inicio de sesion. Intenta nuevamente mas tarde.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static final class Ventana {
        private final long minuto;
        private final AtomicInteger intentos = new AtomicInteger(1);
        private Ventana(long minuto) { this.minuto = minuto; }
    }
}
