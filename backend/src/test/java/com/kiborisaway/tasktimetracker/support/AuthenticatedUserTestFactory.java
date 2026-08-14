package com.kiborisaway.tasktimetracker.support;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;

/**
 * テスト用の {@link AuthenticatedUser} を生成するファクトリです。
 */
public final class AuthenticatedUserTestFactory {

  private AuthenticatedUserTestFactory() {
  }

  public static AuthenticatedUser create(int userId) {
    return create(userId, "user" + userId + "@example.com", false);
  }

  public static AuthenticatedUser create(int userId, String email, boolean passwordChangeRequired) {
    AppUser appUser = new AppUser();
    appUser.setId(userId);
    appUser.setEmail(email);
    appUser.setPasswordHash("{bcrypt}$2a$12$dummydummydummydummydummydummydummydummydummydu");
    appUser.setIsEnabled(true);
    appUser.setPasswordChangeRequired(passwordChangeRequired);
    return new AuthenticatedUser(appUser);
  }
}
