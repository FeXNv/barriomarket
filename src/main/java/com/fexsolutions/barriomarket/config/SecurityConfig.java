package com.fexsolutions.barriomarket.config;

import com.fexsolutions.barriomarket.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filtro(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)          // API stateless con token en cabecera
            .headers(h -> h
            .contentSecurityPolicy(c -> c.policyDirectives(
                "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
        +       "frame-ancestors 'none'; base-uri 'self'; form-action 'self'"))
            .frameOptions(f -> f.deny()))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/register", "/api/auth/login", "/actuator/health").permitAll()
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/img/**", "/icons/**",
                "/sw.js", "/manifest.webmanifest", "/favicon.ico").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}