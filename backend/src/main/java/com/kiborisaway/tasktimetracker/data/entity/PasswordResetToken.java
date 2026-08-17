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
public class PasswordResetToken {

  private Integer id;

  private Integer userId;

  private String tokenHash;

  private LocalDateTime expiresAt;

  private LocalDateTime usedAt;

  private LocalDateTime createdAt;
}
