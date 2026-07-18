package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.kiborisaway.tasktimetracker.data.Project;
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

  @Autowired
  private ProjectRepository sut;


  @Test
  void 全件検索_初期データに含まれるプロジェクトを取得できること() {
    // Act
    List<Project> actual = sut.findAll();

    // Assert
    assertThat(actual)
        .extracting(Project::getTitle, Project::getDescription, Project::getIsFinished)
        .containsExactlyInAnyOrder(
            tuple("タスク管理アプリ開発", "A社から受託した開発", false),
            tuple("Java Silver勉強", null, false),
            tuple("プロジェクトX", "社外秘", true)
        );
  }

  @Test
  void 取り組み中プロジェクト検索_初期データに含まれるisFinishedがfalseのプロジェクトのみをを取得できること() {
    // Act
    List<Project> actual = sut.findAllInProgress();

    // Assert
    assertThat(actual)
        .extracting(Project::getTitle, Project::getDescription, Project::getIsFinished)
        .containsExactlyInAnyOrder(
            tuple("タスク管理アプリ開発", "A社から受託した開発", false),
            tuple("Java Silver勉強", null, false)
        );
  }

  @Test
  void ID検索成功_IDが一致するプロジェクトを取得できること() {
    // Arrange
    int id = 1;

    Project expected = sut.findAll().stream()
        .filter(p -> p.getId().equals(id))
        .findFirst()
        .orElseThrow();

    // Act
    Project actual = sut.findById(id);

    // Assert
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void ID検索失敗_存在しないIDを指定するとnullを返すこと() {
    // Arrange
    int id = 999;

    // Act
    Project actual = sut.findById(id);

    // Assert
    assertThat(actual).isNull();
  }

  @ParameterizedTest(name = "[{index}]登録成功_プロジェクトを登録でき採番されたidが設定されること_完了フラグは{0}を指定するとfalseで登録されること")
  @ValueSource(booleans = {true, false})
  @NullSource
  void 登録成功_プロジェクトを登録でき採番されたidが設定されること_完了フラグは常にfalseで登録されること(
      Boolean flag) {
    // Arrange
    List<Project> before = sut.findAll();

    Project arg = new Project();
    arg.setTitle("Spring Boot学習");
    arg.setDescription("REST APIを作る");
    arg.setIsFinished(flag);

    // Act
    sut.insert(arg);

    List<Project> after = sut.findAll();
    Project registered = sut.findById(arg.getId());

    // Assert
    assertThat(arg.getId()).isNotNull();
    assertThat(after).hasSize(before.size() + 1);
    assertThat(registered.getTitle()).isEqualTo("Spring Boot学習");
    assertThat(registered.getDescription()).isEqualTo("REST APIを作る");
    assertThat(registered.getIsFinished()).isEqualTo(false);
  }

  @Test
  void 登録失敗_titleがnullの場合は例外が発生すること() {
    Project project = new Project();
    project.setTitle(null);
    project.setDescription("説明");

    assertThatThrownBy(() -> sut.insert(project))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @ParameterizedTest(name = "[{index}]登録失敗_titleが{0}の場合は例外が発生すること")
  @ValueSource(strings = {"", " "})
  void 登録失敗_titleが有効な文字を含まない場合は例外が発生すること(String invalidTitle) {
    Project project = new Project();
    project.setTitle(invalidTitle);
    project.setDescription("説明");

    assertThatThrownBy(() -> sut.insert(project))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 登録失敗_titleが20文字を超える場合は例外が発生すること() {
    Project project = new Project();
    project.setTitle("123456789012345678901"); // 21文字
    project.setDescription("説明");

    assertThatThrownBy(() -> sut.insert(project))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 更新成功_既存プロジェクトのタイトルと説明と完了フラグを更新できること() {
    // Arrange
    int id = 1;
    // もとは"タスク管理システム開発","A社から受託した開発",false
    Project project = new Project(id, "進捗管理システム開発", "社内向けシステム開発", true);

    // Act
    int actual = sut.update(project);

    // Assert
    assertThat(actual).isEqualTo(1);// 更新件数が１件

    Project updated = sut.findById(id);

    assertThat(updated.getTitle()).isEqualTo("進捗管理システム開発");
    assertThat(updated.getDescription()).isEqualTo("社内向けシステム開発");
    assertThat(updated.getIsFinished()).isEqualTo(true);
  }

  @Test
  void 更新失敗_存在しないIDの場合は更新されず0件となること() {
    // Arrange
    List<Project> before = sut.findAll();

    Project project = new Project();
    project.setId(999); // 存在しないID
    project.setTitle("更新されないタイトル");
    project.setDescription("更新されない説明");
    project.setIsFinished(true);

    // Act
    int actual = sut.update(project);

    // Assert
    assertThat(actual).isEqualTo(0); // 更新件数0件

    List<Project> after = sut.findAll();

    // 件数が変わっていないこと
    assertThat(after).hasSize(before.size());

    // 中身も変わっていないこと
    assertThat(after)
        .usingRecursiveComparison()
        .isEqualTo(before);
  }

  @ParameterizedTest(name = "[{index}]更新失敗_{0}フィールドがnullの場合は例外が発生すること")
  @ValueSource(strings = {"title", "isFinished"})
  void 更新失敗_DBでnull非許容のフィールドがnullの場合はDataIntegrityViolationExceptionが発生すること(
      String field) {
    Project project = new Project();
    project.setId(1);
    project.setTitle(field.equals("title") ? null : "プロジェクトA");
    project.setDescription("説明更新");
    project.setIsFinished(field.equals("isFinished") ? null : false);

    assertThatThrownBy(() -> sut.update(project))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @ParameterizedTest(name = "[{index}]登録失敗_titleが{0}の場合は例外が発生すること")
  @ValueSource(strings = {"", " "})
  void 更新失敗_titleが有効な文字を含まない場合は例外が発生すること(String invalidTitle) {
    Project project = new Project();
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
    project.setTitle("123456789012345678901"); // 21文字
    project.setDescription("説明更新");
    project.setIsFinished(true);

    assertThatThrownBy(() -> sut.update(project))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

}