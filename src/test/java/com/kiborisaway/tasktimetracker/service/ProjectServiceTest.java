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
  void プロジェクト一覧検索_引数にnullを指定すると全件検索用のリポジトリのメソッドを呼び出すこと() {
    // Arrange
    Project project1 = new Project(1, "タスク管理アプリ開発", "A社から受託した開発", false);
    Project project2 = new Project(2, "Java Silver勉強", null, true);

    List<Project> expected = List.of(project1, project2);

    when(repository.findAll()).thenReturn(expected);

    // Act
    List<Project> actual = sut.findAllByCondition(null);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(repository, times(1)).findAll();
  }

  @Test
  void プロジェクト一覧検索_引数にtrueを指定すると完了フラグ指定検索用のリポジトリのメソッドにtrueを指定して呼び出すこと() {
    // Arrange
    Project project = new Project(2, "Java Silver勉強", null, true);

    List<Project> expected = List.of(project);

    when(repository.findAllByIsFinished(true)).thenReturn(expected);

    // Act
    List<Project> actual = sut.findAllByCondition(true);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(repository, times(1)).findAllByIsFinished(true);
  }

  @Test
  void プロジェクト一覧検索_引数にfalseを指定すると完了フラグ指定検索用のリポジトリのメソッドにfalseを指定して呼び出すこと() {
    // Arrange
    Project project = new Project(1, "タスク管理アプリ開発", "A社から受託した開発", false);

    List<Project> expected = List.of(project);

    when(repository.findAllByIsFinished(false)).thenReturn(expected);

    // Act
    List<Project> actual = sut.findAllByCondition(false);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(repository, times(1)).findAllByIsFinished(false);
  }

  @Test
  void ID検索成功_プロジェクトを取得できること() {
    // Arrange
    int id = 1;
    Project expected = new Project(id, "タスク管理アプリ開発", "A社から受託した開発", false);

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
    Project project = new Project(3, "Spring Boot学習", "REST APIを作る", null);

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
    Project project = new Project(1, "タスク管理アプリ開発", "A社から受託した開発", true);

    when(repository.update(project)).thenReturn(1);

    // Act
    sut.update(project);

    // Assert
    verify(repository, times(1)).update(same(project));
  }

  @Test
  void 更新失敗_DB制約違反の例外をそのまま送出すること() {
    // Arrange
    Project project = new Project(1, null, "説明更新", true);

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
    Project project = new Project(999, "更新されないタイトル", "更新されない説明", true);

    when(repository.update(project)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.update(project))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).update(same(project));
  }


}