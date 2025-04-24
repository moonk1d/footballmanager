package com.nazarov.footballmanager.security.jwt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

  // Called when an unauthenticated user tries to access a secured resource
  @Override
  public void commence(HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException) throws IOException, ServletException {
    log.error("Responding with unauthorized error. Message - {}", authException.getMessage());
    // Send standard 401 Unauthorized response
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
  }
}