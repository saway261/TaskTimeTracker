package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.kiborisaway.tasktimetracker.data.entity.Reflection;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@MybatisTest
class ReflectionRepositoryTest {

  private static final int PROJECT_WITH_REFLECTION_FIXTURES = 3;

  @Autowired
  private ReflectionRepository sut;

  @Test
  void タスクID検索_指定したタスクの振り返りを取得できること() {
    Reflection actual = sut.findByTaskId(6);

    assertThat(actual.getId()).isEqualTo(1);
    assertThat(actual.getTaskId()).isEqualTo(6);
    assertThat(actual.getCause()).isEqualTo("着手前の調査が不足していた");
    assertThat(actual.getNextAction()).isEqualTo("類似タスクの実績を見積もり前に確認する");
    assertThat(actual.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 10, 10, 5));
    assertThat(actual.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 10, 10, 5));
  }

  @Test
  void タスクID検索_振り返りが存在しない場合はnullを返すこと() {
    assertThat(sut.findByTaskId(7)).isNull();
  }

  @Test
  void 存在チェック_振り返りの有無を判定できること() {
    assertThat(sut.existsByTaskId(6)).isTrue();
    assertThat(sut.existsByTaskId(7)).isFalse();
  }

  @Test
  void 登録成功_振り返りを登録でき採番されたidと日時が設定されること() {
    Reflection reflection = new Reflection(null, 7, "見積もりが過大だった", null, null, null);

    sut.insert(reflection);

    assertThat(reflection.getId()).isNotNull();
    assertThat(sut.findByTaskId(7))
        .extracting(
            Reflection::getTaskId,
            Reflection::getCause,
            Reflection::getNextAction)
        .containsExactly(7, "見積もりが過大だった", null);
    assertThat(sut.findByTaskId(7).getCreatedAt()).isNotNull();
    assertThat(sut.findByTaskId(7).getUpdatedAt()).isNotNull();
  }

  @Test
  void 登録成功_causeがNULLでも登録できること() {
    Reflection reflection = new Reflection(null, 7, null, null, null, null);

    sut.insert(reflection);

    assertThat(sut.findByTaskId(7).getCause()).isNull();
  }

  @Test
  void 登録失敗_同じタスクIDへ二回目の振り返りを登録すると一意制約違反になること() {
    Reflection duplicate = new Reflection(null, 6, "重複登録", null, null, null);

    assertThatThrownBy(() -> sut.insert(duplicate))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 登録失敗_causeが空白文字だけの場合はチェック制約違反になること() {
    Reflection reflection = new Reflection(null, 7, "   ", null, null, null);

    assertThatThrownBy(() -> sut.insert(reflection))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 更新成功_タスクIDを指定して原因と改善アクションだけを更新できること() {
    Reflection before = sut.findByTaskId(6);
    Reflection reflection = new Reflection(
        999, 6, "更新後の原因", null, LocalDateTime.MIN, LocalDateTime.MIN);

    int actual = sut.updateByTaskId(reflection);

    Reflection updated = sut.findByTaskId(6);
    assertThat(actual).isEqualTo(1);
    assertThat(updated.getId()).isEqualTo(before.getId());
    assertThat(updated.getTaskId()).isEqualTo(before.getTaskId());
    assertThat(updated.getCause()).isEqualTo("更新後の原因");
    assertThat(updated.getNextAction()).isNull();
    assertThat(updated.getCreatedAt()).isEqualTo(before.getCreatedAt());
    assertThat(updated.getUpdatedAt()).isNotNull();
  }

  @Test
  void 更新成功_causeをNULLへ更新できること() {
    Reflection reflection = new Reflection(
        999, 6, null, null, LocalDateTime.MIN, LocalDateTime.MIN);

    sut.updateByTaskId(reflection);

    assertThat(sut.findByTaskId(6).getCause()).isNull();
  }

  @Test
  void 更新失敗_存在しないタスクIDの場合は更新件数0件となること() {
    Reflection reflection = new Reflection(null, 999, "更新されない原因", null, null, null);

    assertThat(sut.updateByTaskId(reflection)).isZero();
  }

  @Test
  void 削除成功_タスクIDを指定して振り返りを削除できること() {
    int actual = sut.deleteByTaskId(6);

    assertThat(actual).isEqualTo(1);
    assertThat(sut.findByTaskId(6)).isNull();
  }

  @Test
  void 削除失敗_存在しないタスクIDの場合は削除件数0件となること() {
    assertThat(sut.deleteByTaskId(999)).isZero();
  }

  @Test
  void プロジェクト直下完了タスク検索_並び順どおりにReflection未入力タスクも取得できること() {
    List<ReflectionTaskRow> actual =
        sut.findDirectTasksInProject(PROJECT_WITH_REFLECTION_FIXTURES);

    assertThat(actual)
        .extracting(
            ReflectionTaskRow::getTaskId,
            ReflectionTaskRow::getTaskTitle,
            ReflectionTaskRow::getReflectionId)
        .containsExactly(
            tuple(7, "振り返り直下B", null),
            tuple(6, "振り返り直下A", 1));
    assertThat(actual).extracting(ReflectionTaskRow::getTaskId).doesNotContain(8);

    ReflectionTaskRow withReflection = actual.get(1);
    assertThat(withReflection.getFinishedAt()).isEqualTo(
        LocalDateTime.of(2026, 8, 10, 10, 0));
    assertThat(withReflection.getActualMinutesCached()).isEqualTo(90);
    assertThat(withReflection.getGapMinutesCached()).isEqualTo(30);
    assertThat(withReflection.getGapRateCached()).isEqualTo(50.0);
    assertThat(withReflection.getCause()).isEqualTo("着手前の調査が不足していた");
    assertThat(withReflection.getNextAction()).isEqualTo("類似タスクの実績を見積もり前に確認する");
    assertThat(withReflection.getReflectionCreatedAt()).isEqualTo(
        LocalDateTime.of(2026, 8, 10, 10, 5));
    assertThat(withReflection.getReflectionUpdatedAt()).isEqualTo(
        LocalDateTime.of(2026, 8, 10, 10, 5));

    ReflectionTaskRow withoutReflection = actual.get(0);
    assertThat(withoutReflection.getCause()).isNull();
    assertThat(withoutReflection.getNextAction()).isNull();
    assertThat(withoutReflection.getReflectionCreatedAt()).isNull();
    assertThat(withoutReflection.getReflectionUpdatedAt()).isNull();
  }

  @Test
  void タスクグループ配下完了タスク検索_グループとタスクの並び順どおりにReflection未入力タスクも取得できること() {
    List<ReflectionTaskRow> actual =
        sut.findGroupedTasksInProject(PROJECT_WITH_REFLECTION_FIXTURES);

    assertThat(actual)
        .extracting(
            ReflectionTaskRow::getTaskGroupId,
            ReflectionTaskRow::getTaskGroupTitle,
            ReflectionTaskRow::getTaskId,
            ReflectionTaskRow::getReflectionId)
        .containsExactly(
            tuple(5, "振り返りグループB", 12, null),
            tuple(4, "振り返りグループA", 10, null),
            tuple(4, "振り返りグループA", 9, 2));
    assertThat(actual).extracting(ReflectionTaskRow::getTaskId).doesNotContain(11);

    ReflectionTaskRow withReflection = actual.get(2);
    assertThat(withReflection.getCause()).isEqualTo("レビュー観点に漏れがあった");
    assertThat(withReflection.getNextAction()).isNull();
    assertThat(withReflection.getReflectionCreatedAt()).isEqualTo(
        LocalDateTime.of(2026, 8, 12, 12, 5));
    assertThat(withReflection.getReflectionUpdatedAt()).isEqualTo(
        LocalDateTime.of(2026, 8, 12, 12, 5));
  }
}
