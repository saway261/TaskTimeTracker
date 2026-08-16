package com.kiborisaway.tasktimetracker.exception;

public class EmailChangeNotAllowedException extends RuntimeException {

  private final boolean credentialFailure;

  public EmailChangeNotAllowedException(String message) {
    this(message, false);
  }

  private EmailChangeNotAllowedException(String message, boolean credentialFailure) {
    super(message);
    this.credentialFailure = credentialFailure;
  }

  public static EmailChangeNotAllowedException currentPasswordIncorrect() {
    return new EmailChangeNotAllowedException("current password is incorrect", true);
  }

  public static EmailChangeNotAllowedException sameAsCurrentEmail() {
    return new EmailChangeNotAllowedException("new email must be different");
  }

  /**
   * 現在のパスワード照合に失敗したことによる例外かどうかを返します。
   * 仕様書14章により、この失敗はログインと同じレート制限対象に含める必要があります。
   */
  public boolean isCredentialFailure() {
    return credentialFailure;
  }
}
