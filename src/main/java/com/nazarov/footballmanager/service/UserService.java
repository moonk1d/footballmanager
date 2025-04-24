package com.nazarov.footballmanager.service;

import com.nazarov.footballmanager.domain.Role;
import com.nazarov.footballmanager.domain.User;
import com.nazarov.footballmanager.dto.user.UserViewDto;
import com.nazarov.footballmanager.exception.ResourceNotFoundException;
import com.nazarov.footballmanager.repository.UserRepository;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public UserViewDto getCurrentUserProfile() {
    User currentUser = getCurrentUserEntity();
    return mapUserToUserViewDto(currentUser);
  }

  // Helper method to get the currently authenticated User entity
  public User getCurrentUserEntity() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
      throw new IllegalStateException("No authenticated user found"); // Or handle differently
    }

    String email;
    Object principal = authentication.getPrincipal();
    if (principal instanceof UserDetails) {
      email = ((UserDetails) principal).getUsername();
    } else {
      email = principal.toString();
    }

    return userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
  }


  // Helper method for mapping
  private UserViewDto mapUserToUserViewDto(User user) {
    return UserViewDto.builder()
        .id(user.getUserId())
        .name(user.getName())
        .email(user.getEmail())
        .dateOfBirth(user.getDateOfBirth())
        .playingPosition(user.getPlayingPosition())
        .profilePictureUrl(user.getProfilePictureUrl())
        .contactNumber(user.getContactNumber())
        .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
        .build();
  }

  // Add other user-related service methods here (e.g., update profile)
}