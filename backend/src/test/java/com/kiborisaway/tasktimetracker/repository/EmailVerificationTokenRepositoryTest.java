package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kiborisaway.tasktimetracker.data.entity.EmailVerificationToken;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

@MybatisTest
class EmailVerificationTokenRepositoryTest {

  private static final int USER_A = 1;
  private static final int USER_B = 2;
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 15, 9, 0);

  @Autowired
  private EmailVerificationTokenRepository sut;

  @Test
  void 登録成功_採番されたIDを設定できること() {
    EmailVerificationToken token = new EmailVerificationToken(
        null, USER_A, "hash-1", NOW.plusHours(24), null, NOW);

    sut.insert(token);

    assertThat(token.getId()).isNotNull();
  }

  @Test
  void 有効トークン検索_ハッシュが一致し未使用かつ期限内なら取得できること() {
    sut.insert(new EmailVerificationToken(null, USER_A, "hash-valid", NOW.plusHours(24), null, NOW));

    EmailVerificationToken actual = sut.findByTokenHashForUpdate("hash-valid");

    assertThat(actual).isNotNull();
    assertThat(actual.getUserId()).isEqualTo(USER_A);
    assertThat(actual.getUsedAt()).isNull();
  }

  @Test
  void 有効トークン検索_存在しないハッシュならnullを返すこと() {
    EmailVerificationToken actual = sut.findByTokenHashForUpdate("unknown-hash");

    assertThat(actual).isNull();
  }

  @Test
  void 一括無効化_対象ユーザーの未使用トークンだけを無効化すること() {
    sut.insert(new EmailVerificationToken(null, USER_A, "hash-a-1", NOW.plusHours(24), null, NOW));
    sut.insert(new EmailVerificationToken(null, USER_A, "hash-a-2", NOW.plusHours(24), null, NOW));
    sut.insert(new EmailVerificationToken(null, USER_B, "hash-b-1", NOW.plusHours(24), null, NOW));
    LocalDateTime usedAt = NOW.plusMinutes(5);

    int actual = sut.invalidateAllForUser(USER_A, usedAt);

    assertThat(actual).isEqualTo(2);
    assertThat(sut.findByTokenHashForUpdate("hash-a-1").getUsedAt()).isEqualTo(usedAt);
    assertThat(sut.findByTokenHashForUpdate("hash-a-2").getUsedAt()).isEqualTo(usedAt);
    assertThat(sut.findByTokenHashForUpdate("hash-b-1").getUsedAt()).isNull();
  }
}
