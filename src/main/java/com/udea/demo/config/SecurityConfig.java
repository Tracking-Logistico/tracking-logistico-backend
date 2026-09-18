package com.udea.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
            .addFilterBefore(sesionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/login",
                                                "/api/v1/auth/refresh",
                                                "/api/v1/auth/password/**",
                                                "/api/v1/clientes/registro",
                                                "/api/v1/clientes/verificar",
                                                "/h2-console/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/v3/api-docs/**")
                .permitAll()
                .requestMatchers("/api/v1/panel/cliente/**").hasRole("CLIENTE")
                .requestMatchers("/api/v1/panel/operador/**").hasRole("OPERADOR")
                .requestMatchers("/api/v1/panel/conductor/**").hasRole("CONDUCTOR")
                .anyRequest().authenticated()
            );
        return http.build();
    }
}