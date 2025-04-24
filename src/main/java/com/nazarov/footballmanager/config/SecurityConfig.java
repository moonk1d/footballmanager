package com.nazarov.footballmanager.config;

import com.nazarov.footballmanager.security.UserDetailsServiceImpl;
import com.nazarov.footballmanager.security.jwt.JwtAuthenticationEntryPoint;
import com.nazarov.footballmanager.security.jwt.JwtAuthenticationFilter;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Enable @PreAuthorize, @PostAuthorize etc.
@RequiredArgsConstructor
public class SecurityConfig {

  private final UserDetailsServiceImpl userDetailsService;
  private final JwtAuthenticationEntryPoint unauthorizedHandler;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Value("${server.port}")
  private String serverPort;

  // Define public endpoints that don't require authentication
  private static final String[] PUBLIC_MATCHERS = {
      "/api/auth/**",
      "/v3/api-docs/**", // OpenAPI spec
      "/swagger-ui/**", // Swagger UI
      "/swagger-ui.html",
  };

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        // Disable CSRF protection as we are using stateless JWT authentication
        .csrf(AbstractHttpConfigurer::disable)
        // Configure CORS (Cross-Origin Resource Sharing) - customize as needed
        // .cors(Customizer.withDefaults()) // Requires a CorsConfigurationSource bean
        // Set exception handling for unauthorized access
        .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
        // Configure session management to be stateless
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Define authorization rules for HTTP requests
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(PUBLIC_MATCHERS).permitAll() // Allow public access to specified paths
            .anyRequest().authenticated() // Require authentication for any other request
        );

    // Set the custom authentication provider
    http.authenticationProvider(authenticationProvider());

    // Add the JWT filter before the standard UsernamePasswordAuthenticationFilter
    http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:" + serverPort)); // Allow frontend origin
        configuration.setAllowedMethods(Arrays.asList("GET","POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}