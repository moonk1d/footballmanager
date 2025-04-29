package com.nazarov.footballmanager.dto.user;

import java.time.LocalDate;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserViewDto {
  private Integer id;
  private String name;
  private String email;
  private LocalDate dateOfBirth;
  private String playingPosition;
  private String profilePictureUrl;
  private String contactNumber;
  private Set<String> roles;
}