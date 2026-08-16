package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kiborisaway.tasktimetracker.data.entity.EmailChangeRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

@MybatisTest
class EmailChangeRequestRepositoryTest {

  private static final int USER_A = 1;
  private static final int USER_B = 2;
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 15, 9, 0);

  @Autowired
  private EmailChangeRequestRepository sut;

  @Test
  void 登録成功_採番されたIDを設定できること() {
    EmailChangeRequest request = new EmailChangeRequest(
        null, USER_A, "new@example.com", "hash-1", NOW.plusHours(24), null, NOW);

    sut.insert(request);

    assertThat(request.getId()).isNotNull();
  }

  @Test
  void 有効要求検索_未使用かつ期限内なら取得できること() {
    sut.insert(new EmailChangeRequest(
        null, USER_A, "new@example.com", "hash-valid", NOW.plusHours(24), null, NOW));

    EmailChangeRequest actual = sut.findValidForUpdate("hash-valid", NOW);

    assertThat(actual).isNotNull();
    assertThat(actual.getNewEmail()).isEqualTo("new@example.com");
  }

  @Test
  void 有効要求検索_期限切れなら取得できないこと() {
    sut.insert(new EmailChangeRequest(
        null, USER_A, "new@example.com", "hash-expired", NOW.minusMinutes(1), null,
        NOW.minusHours(25)));

    EmailChangeRequest actual = sut.findValidForUpdate("hash-expired", NOW);

    assertThat(actual).isNull();
  }

  @Test
  void 有効要求検索_使用済みなら取得できないこと() {
    EmailChangeRequest request = new EmailChangeRequest(
        null, USER_A, "new@example.com", "hash-used", NOW.plusHours(24), null, NOW);
    sut.insert(request);
    sut.markUsed(request.getId(), NOW);

    EmailChangeRequest actual = sut.findValidForUpdate("hash-used", NOW);

    assertThat(actual).isNull();
  }

  @Test
  void 個別無効化_指定した要求だけを使用済みにすること() {
    EmailChangeRequest request = new EmailChangeRequest(
        null, USER_A, "new@example.com", "hash-mark", NOW.plusHours(24), null, NOW);
    sut.insert(request);
    LocalDateTime usedAt = NOW.plusMinutes(5);

    int actual = sut.markUsed(request.getId(), usedAt);

    assertThat(actual).isEqualTo(1);
    assertThat(sut.findValidForUpdate("hash-mark", NOW)).isNull();
  }

  @Test
  void 一括無効化_対象ユーザーの未使用要求だけを無効化すること() {
    sut.insert(new EmailChangeRequest(
        null, USER_A, "a1@example.com", "hash-a-1", NOW.plusHours(24), null, NOW));
    sut.insert(new EmailChangeRequest(
        null, USER_A, "a2@example.com", "hash-a-2", NOW.plusHours(24), null, NOW));
    sut.insert(new EmailChangeRequest(
        null, USER_B, "b1@example.com", "hash-b-1", NOW.plusHours(24), null, NOW));
    LocalDateTime usedAt = NOW.plusMinutes(5);

    int actual = sut.invalidateAllForUser(USER_A, usedAt);

    assertThat(actual).isEqualTo(2);
    assertThat(sut.findValidForUpdate("hash-a-1", NOW)).isNull();
    assertThat(sut.findValidForUpdate("hash-a-2", NOW)).isNull();
    assertThat(sut.findValidForUpdate("hash-b-1", NOW)).isNotNull();
  }
}
