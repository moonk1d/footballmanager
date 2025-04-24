package com.nazarov.footballmanager.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegistrationDto {

  @NotBlank(message = "Name cannot be blank")
  @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
  private String name;

  @NotBlank(message = "Email cannot be blank")
  @Email(message = "Invalid email format")
  @Size(max = 100, message = "Email cannot exceed 100 characters")
  private String email;

  @NotBlank(message = "Password cannot be blank")
  @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
  private String password;

  @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date of birth must be in YYYY-MM-DD format")
  private String dateOfBirth;
  private String playingPosition;
  private String contactNumber;
}