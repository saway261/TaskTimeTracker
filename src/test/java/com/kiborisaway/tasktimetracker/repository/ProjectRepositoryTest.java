package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.kiborisaway.tasktimetracker.data.Project;
import java.util.List;
import org.junit.jupiter.api.Test;
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
    List<Project> actual = sut.searchProjectList();

    // Assert
    assertThat(actual)
        .extracting(Project::getTitle, Project::getDescription)
        .contains(
            tuple("タスク管理アプリ開発", "A社から受託した開発"),
            tuple("Java Silver勉強", null)
        );
  }

  @Test
  void ID検索成功_IDが一致するプロジェクトを取得できること(){
    // Arrange
    int id = 1;

    Project expected = sut.searchProjectList().stream()
        .filter(p -> p.getId().equals(id))
        .findFirst()
        .orElseThrow();

    // Act
    Project actual = sut.searchProjectById(id);

    // Assert
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void ID検索失敗_存在しないIDを指定するとnullを返すこと(){
    // Arrange
    int id = 999;

    // Act
    Project actual = sut.searchProjectById(id);

    // Assert
    assertThat(actual).isNull();
  }

  @Test
  void 登録成功_プロジェクトを登録でき採番されたidが設定されること() {
    // Arrange
    List<Project> before = sut.searchProjectList();

    Project arg = new Project();
    arg.setTitle("Spring Boot学習");
    arg.setDescription("REST APIを作る");

    // Act
    sut.registerProject(arg);

    List<Project> after = sut.searchProjectList();
    Project registered = sut.searchProjectById(arg.getId());

    // Assert
    assertThat(arg.getId()).isNotNull();
    assertThat(after).hasSize(before.size() + 1);
    assertThat(registered.getTitle()).isEqualTo("Spring Boot学習");
    assertThat(registered.getDescription()).isEqualTo("REST APIを作る");
  }

  @Test
  void 登録失敗_titleがnullの場合は例外が発生すること() {
    Project project = new Project();
    project.setTitle(null);
    project.setDescription("説明");

    assertThatThrownBy(() -> sut.registerProject(project))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 登録失敗_titleが20文字を超える場合は例外が発生すること() {
    Project project = new Project();
    project.setTitle("123456789012345678901"); // 21文字
    project.setDescription("説明");

    assertThatThrownBy(() -> sut.registerProject(project))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 更新成功_既存プロジェクトのタイトルと説明を更新できること() {
    // Arrange
    int id = 1;
    Project project = new Project();
    project.setId(id);
    project.setTitle("進捗管理システム開発");//もとは「タスク管理システム開発」
    project.setDescription("社内向けシステム開発");//もとは「A社から受託した開発」

    // Act
    int actual = sut.updateProject(project);

    // Assert
    assertThat(actual).isEqualTo(1);// 更新件数が１件

    Project updated = sut.searchProjectById(id);

    assertThat(updated.getTitle()).isEqualTo("進捗管理システム開発");
    assertThat(updated.getDescription()).isEqualTo("社内向けシステム開発");
  }

  @Test
  void 更新失敗_存在しないIDの場合は更新されず0件となること() {
    // Arrange
    List<Project> before = sut.searchProjectList();

    Project project = new Project();
    project.setId(999); // 存在しないID
    project.setTitle("更新されないタイトル");
    project.setDescription("更新されない説明");

    // Act
    int actual = sut.updateProject(project);

    // Assert
    assertThat(actual).isEqualTo(0); // 更新件数0件

    List<Project> after = sut.searchProjectList();

    // 件数が変わっていないこと
    assertThat(after).hasSize(before.size());

    // 中身も変わっていないこと
    assertThat(after)
        .usingRecursiveComparison()
        .isEqualTo(before);
  }

  @Test
  void 更新失敗_titleがnullの場合は例外が発生すること() {
    Project project = new Project();
    project.setId(1);
    project.setTitle(null);
    project.setDescription("説明更新");

    assertThatThrownBy(() -> sut.updateProject(project))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 更新失敗_titleが20文字を超える場合は例外が発生すること() {
    Project project = new Project();
    project.setId(1);
    project.setTitle("123456789012345678901"); // 21文字
    project.setDescription("説明更新");

    assertThatThrownBy(() -> sut.updateProject(project))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

}