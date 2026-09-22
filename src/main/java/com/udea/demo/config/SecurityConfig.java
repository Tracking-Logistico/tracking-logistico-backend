package com.udea.demo.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final SesionAuthenticationFilter sesionAuthenticationFilter;

    public SecurityConfig(SesionAuthenticationFilter sesionAuthenticationFilter) {
        this.sesionAuthenticationFilter = sesionAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Stateless Bearer authentication; no cookie-based login.
            .cors(Customizer.withDefaults())
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(
                    org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, error) -> {
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"error\":\"Se requiere una sesión válida\"}");
                })
                .accessDeniedHandler((request, response, error) -> {
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"error\":\"No tienes permiso para realizar esta acción\"}");
                }))
            .addFilterBefore(sesionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error", "/api/v1/auth/login", "/api/v1/auth/refresh",
                                 "/api/v1/auth/password/**", "/api/v1/clientes/registro",
                                 "/api/v1/clientes/verificar", "/h2-console/**",
                                 "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/v1/auth/logout").authenticated()
                .requestMatchers("/api/v1/pedidos/**").hasRole("OPERADOR")
                .requestMatchers("/api/v1/rutas/envios-pendientes", "/api/v1/rutas/asignaciones",
                                 "/api/v1/rutas/asignaciones/reasignar", "/api/v1/rutas/*/orden")
                    .hasRole("OPERADOR")
                .requestMatchers("/api/v1/rutas/conductores/**").hasAnyRole("OPERADOR", "CONDUCTOR")
                .requestMatchers("/api/v1/clientes/*/perfil", "/api/v1/clientes/*")
                    .hasRole("CLIENTE")
                .requestMatchers("/api/v1/usuarios/*/password")
                    .hasAnyRole("CLIENTE", "OPERADOR", "CONDUCTOR")
                .requestMatchers("/api/v1/panel/cliente/**").hasRole("CLIENTE")
                .requestMatchers("/api/v1/panel/operador/**").hasRole("OPERADOR")
                .requestMatchers("/api/v1/panel/conductor/**").hasRole("CONDUCTOR")
                .anyRequest().denyAll()
            );
        return http.build();
    }
}
