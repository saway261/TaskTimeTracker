package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.kiborisaway.tasktimetracker.data.entity.CauseDirection;
import com.kiborisaway.tasktimetracker.data.entity.ReflectionCauseCategory;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

@MybatisTest
class ReflectionCauseCategoryRepositoryTest {

  @Autowired
  private ReflectionCauseCategoryRepository sut;

  @Test
  void 有効カテゴリ検索_表示順に16件の有効カテゴリを取得できること() {
    List<ReflectionCauseCategory> actual = sut.findAllActive();

    assertThat(actual).hasSize(16);
    assertThat(actual)
        .isSortedAccordingTo(Comparator.comparing(ReflectionCauseCategory::getDisplayOrder));
    assertThat(actual.get(0))
        .extracting(
            ReflectionCauseCategory::getCode,
            ReflectionCauseCategory::getDirection,
            ReflectionCauseCategory::getDisplayOrder)
        .containsExactly("TASK_BREAKDOWN", CauseDirection.OVER, 10);
    assertThat(actual.get(actual.size() - 1).getCode()).isEqualTo("OTHER");
  }

  @Test
  void 有効カテゴリ検索_OTHERのみrequiresCauseがtrueであること() {
    List<ReflectionCauseCategory> actual = sut.findAllActive();

    assertThat(actual)
        .filteredOn(category -> Boolean.TRUE.equals(category.getRequiresCause()))
        .extracting(ReflectionCauseCategory::getCode)
        .containsExactly("OTHER");
  }

  @Test
  void 有効カテゴリ検索_無効化されたカテゴリは含まれないこと() {
    List<ReflectionCauseCategory> actual = sut.findAllActive();

    assertThat(actual)
        .extracting(ReflectionCauseCategory::getCode)
        .doesNotContain("TEST_INACTIVE");
  }

  @Test
  void コード検索成功_有効なコードを指定するとカテゴリを取得できること() {
    ReflectionCauseCategory actual = sut.findActiveByCode("CONDITION");

    assertThat(actual.getLabel()).isEqualTo("体調・コンディション");
    assertThat(actual.getDirection()).isEqualTo(CauseDirection.BOTH);
    assertThat(actual.getNextActionHint()).isEqualTo("その条件が何だったかを記録して再現するか、予防する");
    assertThat(actual.getIsActive()).isTrue();
    assertThat(actual.getRequiresCause()).isFalse();
  }

  @Test
  void コード検索成功_ゴール不明確カテゴリは超過短縮共通であること() {
    ReflectionCauseCategory actual = sut.findActiveByCode("UNCLEAR_GOAL");

    assertThat(actual.getDirection()).isEqualTo(CauseDirection.BOTH);
  }

  @Test
  void コード検索成功_nextActionHintが未設定のカテゴリはnullで取得できること() {
    ReflectionCauseCategory actual = sut.findActiveByCode("OTHER");

    assertThat(actual.getNextActionHint()).isNull();
    assertThat(actual.getDirection()).isEqualTo(CauseDirection.BOTH);
    assertThat(actual.getRequiresCause()).isTrue();
  }

  @Test
  void コード検索失敗_存在しないコードの場合はnullを返すこと() {
    assertThat(sut.findActiveByCode("UNKNOWN_CODE")).isNull();
  }

  @Test
  void コード検索失敗_無効化されたコードの場合はnullを返すこと() {
    assertThat(sut.findActiveByCode("TEST_INACTIVE")).isNull();
  }
}
