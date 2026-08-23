package com.kiborisaway.tasktimetracker.data.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class AppUser {

  private Integer id;

  private String email;

  private String passwordHash;

  private Boolean isEnabled;

  private Boolean passwordChangeRequired;

  private LocalDateTime temporaryPasswordExpiresAt;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  private LocalDateTime emailVerifiedAt;

  private Boolean onboardingCompleted;
}
