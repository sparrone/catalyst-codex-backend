package com.separrone.awakeningbackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.separrone.awakeningbackend.dto.UserSettingsDTO;
import com.separrone.awakeningbackend.entity.User;
import com.separrone.awakeningbackend.repository.UserRepository;
import com.separrone.awakeningbackend.service.DatabaseUserDetailsService;
import com.separrone.awakeningbackend.service.UserSettingsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class SecurityConfig {

    private final DatabaseUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final UserSettingsService userSettingsService;
    private final ObjectMapper objectMapper;

    public SecurityConfig(DatabaseUserDetailsService userDetailsService,
                          UserRepository userRepository,
                          UserSettingsService userSettingsService,
                          ObjectMapper objectMapper) {
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.userSettingsService = userSettingsService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // enable in prod if possible
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/users/**").permitAll()
                        .requestMatchers("/auth/register", "/auth/verify").permitAll()
                        .requestMatchers(HttpMethod.GET, "/forum/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/forum/threads/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/forum/posts/**").permitAll()
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginProcessingUrl("/auth/login")
                        .successHandler((request, response, authentication) -> {
                            response.setContentType("application/json");
                            
                            // Get user and settings
                            String username = authentication.getName();
                            User user = userRepository.findByUsername(username).orElse(null);
                            UserSettingsDTO settings = user != null 
                                ? userSettingsService.getUserSettings(user)
                                : UserSettingsDTO.getDefaults();
                            
                            // Create response
                            Map<String, Object> loginResponse = new HashMap<>();
                            loginResponse.put("message", "Login successful");
                            loginResponse.put("username", username);
                            loginResponse.put("settings", settings);
                            
                            String jsonResponse = objectMapper.writeValueAsString(loginResponse);
                            response.getWriter().write(jsonResponse);
                            response.setStatus(HttpServletResponse.SC_OK);
                        })
                        .failureHandler((request, response, exception) -> {
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Invalid username or password.\"}");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Logout successful.\"}");
                            response.setStatus(HttpServletResponse.SC_OK);
                        })
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Environment-aware CORS origins
        String activeProfile = System.getProperty("spring.profiles.active", "dev");
        if ("prod".equals(activeProfile)) {
            // Production origins
            config.setAllowedOrigins(List.of(
                "https://awakening.hosting.com", // Production frontend domain
                "https://awakening-frontend.onrender.com" // Alternative if using Render for frontend
            ));
        } else if ("dev-remote".equals(activeProfile)) {
            // Remote development environment origins
            config.setAllowedOrigins(List.of(
                "https://awakening-frontend-dev.onrender.com" // Remote dev frontend
            ));
        } else {
            // Local development origins
            config.setAllowedOrigins(List.of(
                "http://localhost:5173", // Local Vite dev server
                "http://localhost:5174", // Alternative local port
                "http://localhost:3000"  // Alternative React dev server port
            ));
        }
        
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("JSESSIONID");
        serializer.setUseHttpOnlyCookie(true);
        
        // Environment-aware cookie security
        String activeProfile = System.getProperty("spring.profiles.active", "dev");
        if ("prod".equals(activeProfile)) {
            serializer.setUseSecureCookie(true); // Require HTTPS in production
            serializer.setSameSite("Strict"); // Stricter CSRF protection in production
        } else if ("dev-remote".equals(activeProfile)) {
            serializer.setUseSecureCookie(true); // Require HTTPS for remote dev (Render uses HTTPS)
            serializer.setSameSite("Lax"); // More lenient for development testing
        } else {
            serializer.setUseSecureCookie(false); // Allow HTTP on localhost
            serializer.setSameSite("Lax"); // More lenient for local development
        }
        
        serializer.setCookieMaxAge(7 * 24 * 60 * 60); // 7 days
        return serializer;
    }
}