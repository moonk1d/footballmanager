package com.nazarov.footballmanager.service;

import com.nazarov.footballmanager.domain.Role;
import com.nazarov.footballmanager.domain.User;
import com.nazarov.footballmanager.dto.user.JwtAuthenticationResponseDto;
import com.nazarov.footballmanager.dto.user.LoginRequestDto;
import com.nazarov.footballmanager.dto.user.UserRegistrationDto;
import com.nazarov.footballmanager.exception.BadRequestException;
import com.nazarov.footballmanager.exception.ResourceNotFoundException;
import com.nazarov.footballmanager.repository.RoleRepository;
import com.nazarov.footballmanager.repository.UserRepository;
import com.nazarov.footballmanager.security.jwt.JwtTokenProvider;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider tokenProvider;

  private static final String DEFAULT_USER_ROLE = "ROLE_USER";

  @Transactional
  public User registerUser(UserRegistrationDto registrationDto) {
    log.info("Attempting to register user with email: {}", registrationDto.getEmail());

    if (userRepository.existsByEmail(registrationDto.getEmail())) {
      log.warn("Email {} already exists.", registrationDto.getEmail());
      throw new BadRequestException("Email address already in use!");
    }

    // Find the default role
    Role userRole = roleRepository.findByName(DEFAULT_USER_ROLE)
        .orElseThrow(() -> new ResourceNotFoundException("Role not set: " + DEFAULT_USER_ROLE));

    LocalDate dob = null;
    if (registrationDto.getDateOfBirth() != null && !registrationDto.getDateOfBirth().isBlank()) {
      try {
        dob = LocalDate.parse(registrationDto.getDateOfBirth());
        if (dob.isAfter(LocalDate.now())) {
          throw new BadRequestException("Date of birth must be in the past or present.");
        }
      } catch (DateTimeParseException e) {
        log.error("Date parsing failed despite pattern validation for input: {}",
            registrationDto.getDateOfBirth(), e);
        throw new BadRequestException("Invalid date of birth format provided.");
      }
    }

    // Create new user's account
    User user = User.builder()
        .name(registrationDto.getName())
        .email(registrationDto.getEmail())
        .password(passwordEncoder.encode(registrationDto.getPassword()))
        .dateOfBirth(dob)
        .playingPosition(registrationDto.getPlayingPosition())
        .contactNumber(registrationDto.getContactNumber())
        .roles(Collections.singleton(userRole))
        .build();

    User savedUser = userRepository.save(user);
    log.info("User registered successfully with email: {}", registrationDto.getEmail());
    return savedUser;
  }

  public JwtAuthenticationResponseDto loginUser(LoginRequestDto loginRequestDto) {
    log.info("Attempting login for user: {}", loginRequestDto.getEmail());

    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginRequestDto.getEmail(),
            loginRequestDto.getPassword()
        )
    );

    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = tokenProvider.generateToken(authentication);

    log.info("User {} logged in successfully.", loginRequestDto.getEmail());
    return new JwtAuthenticationResponseDto(jwt);
  }
}