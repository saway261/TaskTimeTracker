package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.kiborisaway.tasktimetracker.data.entity.Project;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@MybatisTest
class ProjectRepositoryTest {

  private static final int USER_A = 1;
  private static final int USER_B = 2;

  @Autowired
  private ProjectRepository sut;


  @Test
  void ユーザー指定全件検索_初期データに含まれるユーザーAのプロジェクトのみを取得できること() {
    // Act
    List<Project> actual = sut.findAllByUserId(USER_A);

    // Assert
    assertThat(actual)
        .extracting(Project::getTitle, Project::getDescription, Project::getIsFinished)
        .containsExactlyInAnyOrder(
            tuple("タスク管理アプリ開発", "A社から受託した開発", false),
            tuple("Java Silver勉強", null, false)
        );
  }

  @Test
  void ユーザー指定全件検索_ID昇順で取得できること() {
    // Act
    List<Project> actual = sut.findAllByUserId(USER_A);

    // Assert
    // プロジェクトには並び順テーブルもcreated_atも無く、この順序が一覧の表示順そのものになる。
    assertThat(actual).extracting(Project::getId).isSorted();
  }

  @Test
  void ユーザー指定全件検索_ユーザーBを指定するとユーザーBのプロジェクトのみを取得できること() {
    // Act
    List<Project> actual = sut.findAllByUserId(USER_B);

    // Assert
    assertThat(actual)
        .extracting(Project::getTitle, Project::getDescription, Project::getIsFinished)
        .containsExactlyInAnyOrder(
            tuple("プロジェクトX", "社外秘", true)
        );
  }

  @Test
  void 完了フラグ指定検索_初期データに含まれるisFinishedがtrueのユーザーBのプロジェクトのみを取得できること() {
    // Act
    List<Project> actual = sut.findAllByIsFinishedAndUserId(true, USER_B);

    // Assert
    assertThat(actual)
        .extracting(Project::getTitle, Project::getDescription, Project::getIsFinished)
        .containsExactlyInAnyOrder(
            tuple("プロジェクトX", "社外秘", true)
        );
  }

  @Test
  void 完了フラグ指定検索_初期データに含まれるisFinishedがfalseのユーザーAのプロジェクトのみを取得できること() {
    // Act
    List<Project> actual = sut.findAllByIsFinishedAndUserId(false, USER_A);

    // Assert
    assertThat(actual)
        .extracting(Project::getTitle, Project::getDescription, Project::getIsFinished)
        .containsExactlyInAnyOrder(
            tuple("タスク管理アプリ開発", "A社から受託した開発", false),
            tuple("Java Silver勉強", null, false)
        );
  }

  @Test
  void 完了フラグ指定検索_所有者が一致しない場合は取得できないこと() {
    // Act
    List<Project> actual = sut.findAllByIsFinishedAndUserId(false, USER_B);

    // Assert
    assertThat(actual).isEmpty();
  }

  @Test
  void ID検索成功_IDと所有者が一致するプロジェクトを取得できること() {
    // Arrange
    int id = 1;

    Project expected = sut.findAllByUserId(USER_A).stream()
        .filter(p -> p.getId().equals(id))
        .findFirst()
        .orElseThrow();

    // Act
    Project actual = sut.findByIdAndUserId(id, USER_A);

    // Assert
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void ID検索失敗_存在しないIDを指定するとnullを返すこと() {
    // Arrange
    int id = 999;

    // Act
    Project actual = sut.findByIdAndUserId(id, USER_A);

    // Assert
    assertThat(actual).isNull();
  }

  @Test
  void ID検索失敗_所有者が一致しない場合はnullを返すこと() {
    // Arrange
    int id = 1;

    // Act
    Project actual = sut.findByIdAndUserId(id, USER_B);

    // Assert
    assertThat(actual).isNull();
  }

  @Test
  void ID存在チェック_存在するIDと所有者が一致するとtrueを返すこと() {
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
    List<Project> before = sut.findAllByUserId(USER_A);

    Project arg = new Project();
    arg.setUserId(USER_A);
    arg.setTitle("Spring Boot学習");
    arg.setDescription("REST APIを作る");
    arg.setIsFinished(flag);

    // Act
    sut.insert(arg);

    List<Project> after = sut.findAllByUserId(USER_A);
    Project registered = sut.findByIdAndUserId(arg.getId(), USER_A);

    // Assert
    assertThat(arg.getId()).isNotNull();
    assertThat(after).hasSize(before.size() + 1);
    assertThat(registered.getTitle()).isEqualTo("Spring Boot学習");
    assertThat(registered.getDescription()).isEqualTo("REST APIを作る");
    assertThat(registered.getIsFinished()).isEqualTo(false);
    assertThat(registered.getUserId()).isEqualTo(USER_A);
  }

  @Test
  void 登録失敗_titleがnullの場合は例外が発生すること() {
    Project project = new Project();
    project.setUserId(USER_A);
    project.setTitle(null);
    project.setDescription("説明");

    assertThatThrownBy(() -> sut.insert(project))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @ParameterizedTest(name = "[{index}]登録失敗_titleが{0}の場合は例外が発生すること")
  @ValueSource(strings = {"", " "})
  void 登録失敗_titleが有効な文字を含まない場合は例外が発生すること(String invalidTitle) {
    Project project = new Project();
    project.setUserId(USER_A);
    project.setTitle(invalidTitle);
    project.setDescription("説明");

    assertThatThrownBy(() -> sut.insert(project))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 登録失敗_titleが20文字を超える場合は例外が発生すること() {
    Project project = new Project();
    project.setUserId(USER_A);
    project.setTitle("123456789012345678901"); // 21文字
    project.setDescription("説明");

    assertThatThrownBy(() -> sut.insert(project))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 更新成功_既存プロジェクトのタイトルと説明を更新でき完了フラグは変更されないこと() {
    // Arrange
    int id = 1;
    // もとは"タスク管理システム開発","A社から受託した開発",false
    Project project = new Project(id, "進捗管理システム開発", "社内向けシステム開発", true);
    project.setUserId(USER_A);

    // Act
    int actual = sut.update(project);

    // Assert
    assertThat(actual).isEqualTo(1);// 更新件数が１件

    Project updated = sut.findByIdAndUserId(id, USER_A);

    assertThat(updated.getTitle()).isEqualTo("進捗管理システム開発");
    assertThat(updated.getDescription()).isEqualTo("社内向けシステム開発");
    assertThat(updated.getIsFinished()).isEqualTo(false);// updateでは変更されない
  }

  @Test
  void 更新失敗_存在しないIDの場合は更新されず0件となること() {
    // Arrange
    List<Project> before = sut.findAllByUserId(USER_A);

    Project project = new Project();
    project.setId(999); // 存在しないID
    project.setUserId(USER_A);
    project.setTitle("更新されないタイトル");
    project.setDescription("更新されない説明");
    project.setIsFinished(true);

    // Act
    int actual = sut.update(project);

    // Assert
    assertThat(actual).isEqualTo(0); // 更新件数0件

    List<Project> after = sut.findAllByUserId(USER_A);

    // 件数が変わっていないこと
    assertThat(after).hasSize(before.size());

    // 中身も変わっていないこと
    assertThat(after)
        .usingRecursiveComparison()
        .isEqualTo(before);
  }

  @Test
  void 更新失敗_所有者が一致しない場合は更新されず0件となること() {
    // Arrange
    int id = 1;
    Project project = new Project(id, "更新されないタイトル", "更新されない説明", true);
    project.setUserId(USER_B);

    // Act
    int actual = sut.update(project);

    // Assert
    assertThat(actual).isEqualTo(0);

    Project unchanged = sut.findByIdAndUserId(id, USER_A);
    assertThat(unchanged.getTitle()).isEqualTo("タスク管理アプリ開発");
  }

  @Test
  void 更新失敗_titleがnullの場合はDataIntegrityViolationExceptionが発生すること() {
    Project project = new Project();
    project.setId(1);
    project.setUserId(USER_A);
    project.setTitle(null);
    project.setDescription("説明更新");

    assertThatThrownBy(() -> sut.update(project))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @ParameterizedTest(name = "[{index}]登録失敗_titleが{0}の場合は例外が発生すること")
  @ValueSource(strings = {"", " "})
  void 更新失敗_titleが有効な文字を含まない場合は例外が発生すること(String invalidTitle) {
    Project project = new Project();
    project.setUserId(USER_A);
    project.setTitle(invalidTitle);
    project.setDescription("説明");
    project.setIsFinished(true);

    assertThatThrownBy(() -> sut.insert(project))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 更新失敗_titleが20文字を超える場合は例外が発生すること() {
    Project project = new Project();
    project.setId(1);
    project.setUserId(USER_A);
    project.setTitle("123456789012345678901"); // 21文字
    project.setDescription("説明更新");
    project.setIsFinished(true);

    assertThatThrownBy(() -> sut.update(project))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 完了状態更新成功_完了フラグを更新でき他のフィールドは変更されないこと() {
    // Arrange
    int id = 1;

    // Act
    int actual = sut.updateFinished(id, true, USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);

    Project updated = sut.findByIdAndUserId(id, USER_A);
    assertThat(updated.getIsFinished()).isTrue();
    assertThat(updated.getTitle()).isEqualTo("タスク管理アプリ開発");
  }

  @Test
  void 完了状態更新失敗_存在しないIDの場合は更新されず0件となること() {
    // Act
    int actual = sut.updateFinished(999, true, USER_A);

    // Assert
    assertThat(actual).isEqualTo(0);
  }

  @Test
  void 完了状態更新失敗_所有者が一致しない場合は更新されず0件となること() {
    // Arrange
    int id = 1;

    // Act
    int actual = sut.updateFinished(id, true, USER_B);

    // Assert
    assertThat(actual).isEqualTo(0);
    Project unchanged = sut.findByIdAndUserId(id, USER_A);
    assertThat(unchanged.getIsFinished()).isFalse();
  }

}
