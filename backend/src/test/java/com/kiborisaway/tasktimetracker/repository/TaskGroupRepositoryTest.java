package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.kiborisaway.tasktimetracker.data.entity.TaskGroup;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@MybatisTest
class TaskGroupRepositoryTest {

  private static final int USER_A = 1;
  private static final int USER_B = 2;

  @Autowired
  private TaskGroupRepository sut;

  @Test
  void 全件検索_指定のプロジェクト配下のタスクグループの初期データを取得できること() {
    // Act
    List<TaskGroup> actual = sut.findAllInProject(1, USER_A);

    // Assert
    assertThat(actual)
        .extracting(TaskGroup::getProjectId, TaskGroup::getTitle, TaskGroup::getDescription,
            TaskGroup::getIsFinished)
        .containsExactlyInAnyOrder(
            tuple(1, "バックエンド開発", null, true),
            tuple(1, "フロントエンド開発", "Reactで", false)
        );
  }

  @Test
  void 全件検索_所有者が一致しない場合は取得できないこと() {
    // Act
    List<TaskGroup> actual = sut.findAllInProject(1, USER_B);

    // Assert
    assertThat(actual).isEmpty();
  }

  @Test
  void 完了フラグ指定検索_指定のプロジェクト配下のタスクグループのうちisFinishedがtrueのプロジェクトのみをを取得できること() {
    // Act
    List<TaskGroup> actual = sut.findAllInProjectByIsFinished(1, true, USER_A);

    // Assert
    assertThat(actual)
        .extracting(TaskGroup::getProjectId, TaskGroup::getTitle, TaskGroup::getDescription,
            TaskGroup::getIsFinished)
        .containsExactlyInAnyOrder(
            tuple(1, "バックエンド開発", null, true)
        );
  }

  @Test
  void 完了フラグ指定検索_指定のプロジェクト配下のタスクグループのうちisFinishedがfalseのプロジェクトのみをを取得できること() {
    // Act
    List<TaskGroup> actual = sut.findAllInProjectByIsFinished(1, false, USER_A);

    // Assert
    assertThat(actual)
        .extracting(TaskGroup::getProjectId, TaskGroup::getTitle, TaskGroup::getDescription,
            TaskGroup::getIsFinished)
        .containsExactlyInAnyOrder(
            tuple(1, "フロントエンド開発", "Reactで", false)
        );
  }

  @Test
  void ID検索成功_IDが一致するタスクグループを取得できること() {
    // Arrange
    int id = 2;

    List<Integer> pIdList = List.of(1, 2, 3);

    TaskGroup expected = pIdList.stream()
        .flatMap(pId -> sut.findAllInProject(pId, USER_A).stream())
        .filter(taskGroup -> taskGroup.getId().equals(id))
        .findFirst()
        .orElseThrow();

    // Act
    TaskGroup actual = sut.findById(id, USER_A);

    // Assert
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void ID検索失敗_存在しないIDを指定するとnullを返すこと() {
    // Arrange
    int id = 999;

    // Act
    TaskGroup actual = sut.findById(id, USER_A);

    // Assert
    assertThat(actual).isNull();
  }

  @Test
  void ID検索失敗_所有者が一致しない場合はnullを返すこと() {
    // Arrange
    int id = 1;

    // Act
    TaskGroup actual = sut.findById(id, USER_B);

    // Assert
    assertThat(actual).isNull();
  }

  @Test
  void ID存在チェック_存在するIDを指定するとtrueを返すこと() {
    // Arrange
    int id = 1;

    // Act
    boolean actual = sut.existsByIdAndUserId(id, USER_A);

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  void ID存在チェック_存在しないIDを指定するとfalseを返すこと() {
    // Arrange
    int id = 999;

    // Act
    boolean actual = sut.existsByIdAndUserId(id, USER_A);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  void ID存在チェック_所有者が一致しない場合はfalseを返すこと() {
    // Arrange
    int id = 1;

    // Act
    boolean actual = sut.existsByIdAndUserId(id, USER_B);

    // Assert
    assertThat(actual).isFalse();
  }

  @ParameterizedTest(name = "[{index}]登録成功_プロジェクトを登録でき採番されたidが設定されること_完了フラグは{0}を指定するとfalseで登録されること")
  @ValueSource(booleans = {true, false})
  @NullSource
  void 登録成功_プロジェクトを登録でき採番されたidが設定されること_完了フラグは常にfalseで登録されること(
      Boolean flag) {
    // Arrange
    int pId = 1;
    List<TaskGroup> before = sut.findAllInProject(pId, USER_A);

    TaskGroup tg = new TaskGroup();
    tg.setProjectId(pId);
    tg.setTitle("デプロイ");
    tg.setDescription("CI・CD設定をも含めた本番リリース");
    tg.setIsFinished(flag);

    // Act
    sut.insert(tg);

    List<TaskGroup> after = sut.findAllInProject(pId, USER_A);
    TaskGroup registered = sut.findById(tg.getId(), USER_A);

    // Assert
    assertThat(tg.getId()).isNotNull();
    assertThat(after).hasSize(before.size() + 1);
    assertThat(registered.getProjectId()).isEqualTo(pId);
    assertThat(registered.getTitle()).isEqualTo("デプロイ");
    assertThat(registered.getDescription()).isEqualTo("CI・CD設定をも含めた本番リリース");
    assertThat(registered.getIsFinished()).isEqualTo(false);
  }

  @ParameterizedTest(name = "[{index}]登録失敗_{0}フィールドがnullの場合は例外が発生すること")
  @ValueSource(strings = {"projectId", "title"})
  void 登録失敗_登録時必須のフィールドがnullの場合はDataIntegrityViolationExceptionが発生すること(
      String field) {
    TaskGroup tg = new TaskGroup();
    tg.setId(1);
    tg.setProjectId(field.equals("projectId") ? null : 1);
    tg.setTitle(field.equals("title") ? null : "プロジェクトA");
    tg.setDescription("説明更新");

    assertThatThrownBy(() -> sut.update(tg, USER_A))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @ParameterizedTest(name = "[{index}]登録失敗_titleが{0}の場合は例外が発生すること")
  @ValueSource(strings = {"", " "})
  void 登録失敗_titleが有効な文字を含まない場合は例外が発生すること(String invalidTitle) {
    TaskGroup tg = new TaskGroup();
    tg.setTitle(invalidTitle);
    tg.setDescription("説明");

    assertThatThrownBy(() -> sut.insert(tg))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 登録失敗_titleが20文字を超える場合は例外が発生すること() {
    TaskGroup tg = new TaskGroup();
    tg.setTitle("123456789012345678901"); // 21文字
    tg.setDescription("説明");

    assertThatThrownBy(() -> sut.insert(tg))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 更新成功_既存プロジェクトのタイトルと説明と完了フラグを更新でき_プロジェクトIDは更新できないこと() {
    // Arrange
    int id = 3;
    // もとは(id,2,'参考書','テキスト通読',true)
    TaskGroup tg = new TaskGroup(id, 3, "進捗管理システム開発", "社内向けシステム開発", false);

    // Act
    int actual = sut.update(tg, USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);// 更新件数が１件

    TaskGroup updated = sut.findById(id, USER_A);

    assertThat(updated.getProjectId()).isEqualTo(2);//更新できず元の値のまま
    assertThat(updated.getTitle()).isEqualTo("進捗管理システム開発");
    assertThat(updated.getDescription()).isEqualTo("社内向けシステム開発");
    assertThat(updated.getIsFinished()).isEqualTo(false);
  }

  @Test
  void 更新失敗_存在しないIDの場合は更新されず0件となること() {
    // Arrange
    int pId = 1;
    List<TaskGroup> before = sut.findAllInProject(pId, USER_A);

    TaskGroup tg = new TaskGroup();
    tg.setId(999); // 存在しないID
    tg.setTitle("更新されないタイトル");
    tg.setDescription("更新されない説明");
    tg.setIsFinished(true);

    // Act
    int actual = sut.update(tg, USER_A);

    // Assert
    assertThat(actual).isEqualTo(0); // 更新件数0件

    List<TaskGroup> after = sut.findAllInProject(pId, USER_A);

    // 中身が変わっていないこと
    assertThat(after)
        .usingRecursiveComparison()
        .isEqualTo(before);
  }

  @Test
  void 更新失敗_所有者が一致しない場合は更新されず0件となること() {
    // Arrange
    int id = 1;
    TaskGroup tg = new TaskGroup(id, 1, "更新されないタイトル", "更新されない説明", true);

    // Act
    int actual = sut.update(tg, USER_B);

    // Assert
    assertThat(actual).isEqualTo(0);

    TaskGroup unchanged = sut.findById(id, USER_A);
    assertThat(unchanged.getTitle()).isEqualTo("バックエンド開発");
  }

  @ParameterizedTest(name = "[{index}]更新失敗_{0}フィールドがnullの場合は例外が発生すること")
  @ValueSource(strings = {"title", "isFinished"})
  void 更新失敗_DBでnull非許容のフィールドがnullの場合はDataIntegrityViolationExceptionが発生すること(
      String field) {
    TaskGroup tg = new TaskGroup();
    tg.setId(1);
    tg.setTitle(field.equals("title") ? null : "プロジェクトA");
    tg.setDescription("説明更新");
    tg.setIsFinished(field.equals("isFinished") ? null : false);

    assertThatThrownBy(() -> sut.update(tg, USER_A))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @ParameterizedTest(name = "[{index}]登録失敗_titleが{0}の場合は例外が発生すること")
  @ValueSource(strings = {"", " "})
  void 更新失敗_titleが有効な文字を含まない場合は例外が発生すること(String invalidTitle) {
    TaskGroup tg = new TaskGroup();
    tg.setTitle(invalidTitle);
    tg.setDescription("説明");
    tg.setIsFinished(true);

    assertThatThrownBy(() -> sut.insert(tg))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 更新失敗_titleが20文字を超える場合は例外が発生すること() {
    TaskGroup tg = new TaskGroup();
    tg.setId(1);
    tg.setTitle("123456789012345678901"); // 21文字
    tg.setDescription("説明更新");
    tg.setIsFinished(true);

    assertThatThrownBy(() -> sut.update(tg, USER_A))
        .isInstanceOf(DataIntegrityViolationException.class);
  }


}
