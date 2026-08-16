package com.kiborisaway.tasktimetracker.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * メールアドレス確認・変更・パスワードリセットで共通して使う、一回限りの乱数トークンの生成とダイジェスト化を行います。
 *
 * <p>生トークンはDB・ログ・例外メッセージに保存しません。DB検索用にはこのクラスが生成するSHA-256ダイジェスト（16進64文字）だけを使用します。
 */
@Component
public class TokenGenerator {

  private static final int TOKEN_BYTES = 32;

  private final SecureRandom secureRandom;

  public TokenGenerator() {
    this(new SecureRandom());
  }

  TokenGenerator(SecureRandom secureRandom) {
    this.secureRandom = secureRandom;
  }

  public String generateRawToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hashed);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }
}
