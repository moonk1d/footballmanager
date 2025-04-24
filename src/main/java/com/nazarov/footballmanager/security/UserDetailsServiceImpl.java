package com.nazarov.footballmanager.security;

import com.nazarov.footballmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return userRepository.findByEmail(email)
        .orElseThrow(() ->
            new UsernameNotFoundException("User not found with email: " + email));
  }

  @Transactional(readOnly = true)
  public UserDetails loadUserById(Long id) {
    return userRepository.findById(id).orElseThrow(
        () -> new UsernameNotFoundException("User not found with id : " + id)
    );
  }
}