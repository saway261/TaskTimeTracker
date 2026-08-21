package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.kiborisaway.tasktimetracker.data.entity.Reflection;
import com.kiborisaway.tasktimetracker.data.entity.ReflectionCauseCategory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@MybatisTest
class ReflectionCauseCategoryLinkRepositoryTest {

  private static final int PROJECT_WITH_REFLECTION_FIXTURES = 3;

  @Autowired
  private ReflectionCauseCategoryLinkRepository sut;

  @Autowired
  private ReflectionCauseCategoryRepository causeCategoryRepository;

  @Autowired
  private ReflectionRepository reflectionRepository;

  @Test
  void 登録成功_1件のリンクを登録して取得できること() {
    int categoryId = categoryId("AS_PLANNED");

    sut.insert(1, categoryId);

    List<ReflectionCauseCategory> actual = sut.findCategoriesByReflectionId(1);
    assertThat(actual).extracting(ReflectionCauseCategory::getCode).containsExactly("AS_PLANNED");
  }

  @Test
  void 登録成功_複数件のリンクを表示順で取得できること() {
    // 登録順はdisplay_order降順にしても、取得結果はdisplay_order昇順で返ることを確認する
    sut.insert(1, categoryId("QUALITY_TRADEOFF")); // display_order=160
    sut.insert(1, categoryId("TASK_BREAKDOWN"));    // display_order=10
    sut.insert(1, categoryId("OTHER"));             // display_order=220

    List<ReflectionCauseCategory> actual = sut.findCategoriesByReflectionId(1);

    assertThat(actual)
        .extracting(ReflectionCauseCategory::getCode)
        .containsExactly("TASK_BREAKDOWN", "QUALITY_TRADEOFF", "OTHER");
  }

  @Test
  void 登録失敗_同じ振り返りへ同じカテゴリを重複登録すると一意制約違反になること() {
    int categoryId = categoryId("TASK_BREAKDOWN");
    sut.insert(1, categoryId);

    assertThatThrownBy(() -> sut.insert(1, categoryId))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 登録失敗_存在しない原因カテゴリIDを指定すると外部キー違反になること() {
    assertThatThrownBy(() -> sut.insert(1, 9999))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 検索成功_リンクを持たない振り返りは空リストを返すこと() {
    assertThat(sut.findCategoriesByReflectionId(2)).isEmpty();
  }

  @Test
  void 全置換_deleteByReflectionIdで全リンクが削除されること() {
    sut.insert(1, categoryId("TASK_BREAKDOWN"));
    sut.insert(1, categoryId("OTHER"));

    int deleted = sut.deleteByReflectionId(1);

    assertThat(deleted).isEqualTo(2);
    assertThat(sut.findCategoriesByReflectionId(1)).isEmpty();
  }

  @Test
  void 全置換_リンクを持たない振り返りに対する削除は0件となること() {
    assertThat(sut.deleteByReflectionId(2)).isZero();
  }

  @Test
  void カスケード削除_振り返りを削除するとリンクも削除されること() {
    sut.insert(1, categoryId("TASK_BREAKDOWN"));

    reflectionRepository.deleteByTaskId(6); // reflection id=1 は task_id=6 に紐づく

    assertThat(sut.findCategoriesByReflectionId(1)).isEmpty();
  }

  @Test
  void プロジェクト内検索_直下とグループ配下のリンクをまとめて取得できること() {
    sut.insert(1, categoryId("TASK_BREAKDOWN")); // reflection id=1 -> task_id=6（直下）
    sut.insert(1, categoryId("OTHER"));
    sut.insert(2, categoryId("FATIGUE"));         // reflection id=2 -> task_id=9（グループ配下）

    List<ReflectionCauseCategoryLinkRow> actual =
        sut.findCategoriesInProject(PROJECT_WITH_REFLECTION_FIXTURES);

    assertThat(actual)
        .extracting(
            ReflectionCauseCategoryLinkRow::getReflectionId,
            ReflectionCauseCategoryLinkRow::getCauseCategoryCode)
        .containsExactly(
            tuple(1, "TASK_BREAKDOWN"),
            tuple(1, "OTHER"),
            tuple(2, "FATIGUE"));
  }

  @Test
  void プロジェクト内検索_他プロジェクトの振り返りのリンクは含まれないこと() {
    Reflection otherProjectReflection = new Reflection(null, 3, "他プロジェクト", null, null, null);
    reflectionRepository.insert(otherProjectReflection);
    sut.insert(otherProjectReflection.getId(), categoryId("TASK_BREAKDOWN"));

    List<ReflectionCauseCategoryLinkRow> actual =
        sut.findCategoriesInProject(PROJECT_WITH_REFLECTION_FIXTURES);

    assertThat(actual)
        .extracting(ReflectionCauseCategoryLinkRow::getReflectionId)
        .doesNotContain(otherProjectReflection.getId());
  }

  @Test
  void プロジェクト内検索_リンクが存在しない場合は空リストを返すこと() {
    assertThat(sut.findCategoriesInProject(PROJECT_WITH_REFLECTION_FIXTURES)).isEmpty();
  }

  private int categoryId(String code) {
    return causeCategoryRepository.findActiveByCode(code).getId();
  }
}
