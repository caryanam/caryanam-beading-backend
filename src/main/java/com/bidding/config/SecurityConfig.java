package com.bidding.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // Public authentication & registration endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/inspector/register",
                                "/api/freelancer/register",
                                "/api/dealer/register").permitAll()

                        // Public image serving & PDF report downloading
                        .requestMatchers(HttpMethod.GET, "/api/inspector/inspection/image/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/inspector/inspection/*/pdf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/inspection/*/pdf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/dealer/inspection/*/pdf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/inspection/*/pdf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/*/inspection/*/pdf").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/ws/auction/**", "/ws/auction").permitAll()
                        .requestMatchers("/api/public/**").permitAll()

                        // Swagger documentation
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/webjars/**").permitAll()

                        // Role based access control
                        .requestMatchers("/api/admin/**", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/inspector/**", "/inspector/**").hasAnyRole("INSPECTOR", "ADMIN")
                        .requestMatchers("/api/freelancer/**", "/freelancer/**").hasAnyRole("FREELANCER", "INSPECTOR", "ADMIN")
                        .requestMatchers("/api/dealer/**", "/dealer/**").hasAnyRole("DEALER", "ADMIN")

                        // No unlisted endpoint is public
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("""
                                    { "success": false, "status": 401,   "message": "Unauthorized. Please provide a valid Bearer token."      }   """);
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("""
                               { "success": false, "status": 403, "message": "Access denied. Your role is not permitted for this API." }   """);
                        }))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
