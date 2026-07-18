package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.Project;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

  @Mock
  private ProjectRepository repository;

  @InjectMocks
  private ProjectService sut;

  @Test
  void 全件検索_プロジェクト一覧を取得できること() {
    // Arrange
    Project project1 = new Project();
    project1.setId(1);
    project1.setTitle("タスク管理アプリ開発");
    project1.setDescription("A社から受託した開発");

    Project project2 = new Project();
    project2.setId(2);
    project2.setTitle("Java Silver勉強");
    project2.setDescription(null);

    List<Project> expected = List.of(project1, project2);

    when(repository.findAll()).thenReturn(expected);

    // Act
    List<Project> actual = sut.findAll();

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(repository, times(1)).findAll();
  }

  @Test
  void ID検索成功_プロジェクトを取得できること() {
    // Arrange
    int id = 1;
    Project expected = new Project();
    expected.setId(id);
    expected.setTitle("タスク管理アプリ開発");
    expected.setDescription("A社から受託した開発");

    when(repository.findById(id)).thenReturn(expected);

    // Act
    Project actual = sut.findById(id);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(repository, times(1)).findById(id);
  }

  @Test
  void ID検索失敗_リポジトリからnullが返ったら例外を投げること() {
    // Arrange
    int id = 999;

    when(repository.findById(id)).thenReturn(null);

    // Assert
    assertThatThrownBy(() -> sut.findById(id))
        .isInstanceOf(TargetNotFoundException.class);
  }

  @Test
  void 登録成功_プロジェクトを登録し同一インスタンスを返すこと() {
    // Arrange
    // ※サービスではINSERT時にidが自動採番されて引数インスタンスに自動でバインドされる挙動を再現できない
    //   かつ、その挙動はリポジトリのテストで検証するので、サービス層のテストでは初めからidを持っておく
    Project project = new Project();
    project.setId(3);
    project.setTitle("Spring Boot学習");
    project.setDescription("REST APIを作る");

    // Act
    Project actual = sut.register(project);

    // Assert
    assertThat(actual).isSameAs(project);
    verify(repository, times(1)).insert(same(project));
  }

  @Test
  void 登録失敗_DB制約違反の例外をそのまま送出すること() {
    // Arrange
    Project project = new Project();
    project.setTitle(null);
    project.setDescription("説明");

    doThrow(new DataIntegrityViolationException("db constraint violation"))
        .when(repository).insert(same(project));

    // Act & Assert
    assertThatThrownBy(() -> sut.register(project))
        .isInstanceOf(DataIntegrityViolationException.class);

    verify(repository, times(1)).insert(same(project));
  }

  @Test
  void 更新成功_既存プロジェクトを更新できること() {
    // Arrange
    Project project = new Project();
    project.setId(1);
    project.setTitle("タスク管理アプリ開発");
    project.setDescription("A社から受託した開発");

    when(repository.update(project)).thenReturn(1);

    // Act
    sut.update(project);

    // Assert
    verify(repository, times(1)).update(same(project));
  }

  @Test
  void 更新失敗_DB制約違反の例外をそのまま送出すること() {
    // Arrange
    Project project = new Project();
    project.setId(1);
    project.setTitle(null);
    project.setDescription("説明更新");

    when(repository.update(project))
        .thenThrow(new DataIntegrityViolationException("db constraint violation"));

    // Act & Assert
    assertThatThrownBy(() -> sut.update(project))
        .isInstanceOf(DataIntegrityViolationException.class);

    verify(repository, times(1)).update(same(project));
  }

  @Test
  void 更新失敗_更新件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    Project project = new Project();
    project.setId(999);
    project.setTitle("更新されないタイトル");
    project.setDescription("更新されない説明");

    when(repository.update(project)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.update(project))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).update(same(project));
  }


}