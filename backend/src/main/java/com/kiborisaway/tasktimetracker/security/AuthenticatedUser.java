package com.kiborisaway.tasktimetracker.security;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 認証済みユーザーを表すPrincipalです。
 *
 * <p>Spring Session JDBCのPRINCIPAL_NAMEを変更されない内部ユーザーIDにするため、
 * {@link #getUsername()} はログインIDのメールアドレスではなくユーザーIDの文字列を返します。
 * ログイン時の検索キーは {@code AppUserDetailsService} が受け取るメールアドレスです。
 */
public class AuthenticatedUser implements UserDetails {

  @Serial
  private static final long serialVersionUID = 1L;

  private final int userId;
  private final String email;
  private final String password;
  private final boolean enabled;
  private final boolean passwordChangeRequired;
  private final LocalDateTime temporaryPasswordExpiresAt;
  private final boolean emailVerified;
  private final boolean onboardingCompleted;

  public AuthenticatedUser(AppUser user) {
    this.userId = user.getId();
    this.email = user.getEmail();
    this.password = user.getPasswordHash();
    this.enabled = Boolean.TRUE.equals(user.getIsEnabled());
    this.passwordChangeRequired = Boolean.TRUE.equals(user.getPasswordChangeRequired());
    this.temporaryPasswordExpiresAt = user.getTemporaryPasswordExpiresAt();
    this.emailVerified = user.getEmailVerifiedAt() != null;
    this.onboardingCompleted = Boolean.TRUE.equals(user.getOnboardingCompleted());
  }

  public int getUserId() {
    return userId;
  }

  public String getEmail() {
    return email;
  }

  public boolean isPasswordChangeRequired() {
    return passwordChangeRequired;
  }

  public LocalDateTime getTemporaryPasswordExpiresAt() {
    return temporaryPasswordExpiresAt;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public boolean isOnboardingCompleted() {
    return onboardingCompleted;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return Integer.toString(userId);
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }
}
