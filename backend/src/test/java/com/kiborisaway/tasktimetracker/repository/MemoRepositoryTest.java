package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.kiborisaway.tasktimetracker.data.entity.Memo;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@MybatisTest
class MemoRepositoryTest {

  private static final int USER_A = 1;
  private static final int USER_B = 2;

  @Autowired
  private MemoRepository sut;

  @Test
  void プロジェクト内メモ検索_指定のプロジェクト配下のメモを取得できること() {
    // Act
    List<Memo> actual = sut.findAllInProject(1);

    // Assert
    assertThat(actual)
        .extracting(Memo::getProjectId, Memo::getTaskGroupId, Memo::getTaskId, Memo::getComment)
        .containsExactly(tuple(1, null, null, "プロジェクト方針を確認する"));
  }

  @Test
  void タスクグループ内メモ検索_指定のタスクグループ配下のメモを取得できること() {
    // Act
    List<Memo> actual = sut.findAllInTaskGroup(1);

    // Assert
    assertThat(actual)
        .extracting(Memo::getProjectId, Memo::getTaskGroupId, Memo::getTaskId, Memo::getComment)
        .containsExactly(tuple(null, 1, null, "バックエンド優先で進める"));
  }

  @Test
  void タスク内メモ検索_指定のタスク配下のメモを取得できること() {
    // Act
    List<Memo> actual = sut.findAllInTask(1);

    // Assert
    assertThat(actual)
        .extracting(Memo::getProjectId, Memo::getTaskGroupId, Memo::getTaskId, Memo::getComment)
        .containsExactly(tuple(null, null, 1, "例外メッセージを見直す"));
  }

  @Test
  void プロジェクト内メモ一括検索_複数プロジェクトのメモを親IDとメモID順で取得できること() {
    sut.insert(new Memo(null, 2, null, null, "プロジェクト2のメモ"));
    sut.insert(new Memo(null, 1, null, null, "プロジェクト1の追加メモ"));

    List<Memo> actual = sut.findAllInProjects(List.of(2, 1));

    assertThat(actual)
        .extracting(Memo::getProjectId, Memo::getComment)
        .containsExactly(
            tuple(1, "プロジェクト方針を確認する"),
            tuple(1, "プロジェクト1の追加メモ"),
            tuple(2, "プロジェクト2のメモ"));
  }

  @Test
  void タスクグループ内メモ一括検索_複数タスクグループのメモを取得できること() {
    sut.insert(new Memo(null, null, 2, null, "タスクグループ2のメモ"));

    List<Memo> actual = sut.findAllInTaskGroups(List.of(1, 2));

    assertThat(actual)
        .extracting(Memo::getTaskGroupId, Memo::getComment)
        .containsExactly(
            tuple(1, "バックエンド優先で進める"),
            tuple(2, "タスクグループ2のメモ"));
  }

  @Test
  void タスク内メモ一括検索_複数タスクのメモを取得できること() {
    sut.insert(new Memo(null, null, null, 2, "タスク2のメモ"));

    List<Memo> actual = sut.findAllInTasks(List.of(1, 2));

    assertThat(actual)
        .extracting(Memo::getTaskId, Memo::getComment)
        .containsExactly(
            tuple(1, "例外メッセージを見直す"),
            tuple(2, "タスク2のメモ"));
  }

  @Test
  void ID検索_指定したメモを取得できること() {
    Memo actual = sut.findById(1);

    assertThat(actual.getId()).isEqualTo(1);
    assertThat(actual.getProjectId()).isEqualTo(1);
    assertThat(actual.getComment()).isEqualTo("プロジェクト方針を確認する");
  }

  @Test
  void 登録成功_プロジェクトIDのみを指定したメモを登録でき採番されたidが設定されること() {
    // Arrange
    Memo memo = new Memo(null, 1, null, null, "追加メモ");

    // Act
    sut.insert(memo);

    // Assert
    assertThat(memo.getId()).isNotNull();
    assertThat(sut.findAllInProject(1))
        .extracting(Memo::getComment)
        .contains("追加メモ");
  }

  @Test
  void 登録失敗_親IDがすべてnullの場合は例外が発生すること() {
    // Arrange
    Memo memo = new Memo(null, null, null, null, "親なしメモ");

    // Assert
    assertThatThrownBy(() -> sut.insert(memo))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 登録失敗_親IDを複数指定した場合は例外が発生すること() {
    // Arrange
    Memo memo = new Memo(null, 1, 1, null, "親重複メモ");

    // Assert
    assertThatThrownBy(() -> sut.insert(memo))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 更新成功_既存メモのコメントだけを更新できること() {
    // Arrange
    Memo memo = new Memo(1, null, null, null, "更新後コメント");

    // Act
    int actual = sut.update(memo, USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);
    assertThat(sut.findAllInProject(1))
        .extracting(Memo::getComment)
        .containsExactly("更新後コメント");
  }

  @Test
  void 更新失敗_存在しないIDの場合は更新されず0件となること() {
    // Arrange
    List<Memo> before = sut.findAllInProject(1);
    Memo memo = new Memo(999, null, null, null, "更新されないコメント");

    // Act
    int actual = sut.update(memo, USER_A);

    // Assert
    assertThat(actual).isEqualTo(0);
    assertThat(sut.findAllInProject(1))
        .usingRecursiveComparison()
        .isEqualTo(before);
  }

  @Test
  void 更新失敗_所有者が一致しない場合は更新されず0件となること() {
    // Arrange
    List<Memo> before = sut.findAllInProject(1);
    Memo memo = new Memo(1, null, null, null, "更新されないコメント");

    // Act
    int actual = sut.update(memo, USER_B);

    // Assert
    assertThat(actual).isEqualTo(0);
    assertThat(sut.findAllInProject(1))
        .usingRecursiveComparison()
        .isEqualTo(before);
  }

  @Test
  void 削除成功_既存メモをID指定で削除できること() {
    // Act
    int actual = sut.delete(1, USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);
    assertThat(sut.findAllInProject(1)).isEmpty();
  }

  @Test
  void 削除失敗_存在しないIDの場合は削除されず0件となること() {
    // Arrange
    List<Memo> before = sut.findAllInProject(1);

    // Act
    int actual = sut.delete(999, USER_A);

    // Assert
    assertThat(actual).isEqualTo(0);
    assertThat(sut.findAllInProject(1))
        .usingRecursiveComparison()
        .isEqualTo(before);
  }

  @Test
  void 削除失敗_所有者が一致しない場合は削除されず0件となること() {
    // Arrange
    List<Memo> before = sut.findAllInProject(1);

    // Act
    int actual = sut.delete(1, USER_B);

    // Assert
    assertThat(actual).isEqualTo(0);
    assertThat(sut.findAllInProject(1))
        .usingRecursiveComparison()
        .isEqualTo(before);
  }

  @Test
  void タスク配下メモ削除成功_指定したタスクIDに紐づくメモを削除できること() {
    // Act
    int actual = sut.deleteAllInTask(1);

    // Assert
    assertThat(actual).isEqualTo(1);
    assertThat(sut.findAllInTask(1)).isEmpty();
  }

  @Test
  void タスク配下メモ削除失敗_存在しないタスクIDの場合は削除されず0件となること() {
    // Arrange
    List<Memo> before = sut.findAllInProject(1);

    // Act
    int actual = sut.deleteAllInTask(999);

    // Assert
    assertThat(actual).isEqualTo(0);
    assertThat(sut.findAllInProject(1))
        .usingRecursiveComparison()
        .isEqualTo(before);
  }
}
