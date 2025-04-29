package com.nazarov.footballmanager.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtAuthenticationResponseDto {
  private String accessToken;
  private String tokenType = "Bearer";

  public JwtAuthenticationResponseDto(String accessToken) {
    this.accessToken = accessToken;
  }
}