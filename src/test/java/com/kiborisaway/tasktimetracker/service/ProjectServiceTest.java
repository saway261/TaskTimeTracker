package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.dto.project.ProjectCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.project.ProjectUpdateRequest;
import com.kiborisaway.tasktimetracker.data.entity.Project;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
  void 登録成功_リクエストの内容でプロジェクトを登録し登録したインスタンスを返すこと() {
    // Arrange
    ProjectCreateRequest request = new ProjectCreateRequest();
    request.setTitle("Spring Boot学習");
    request.setDescription("REST APIを作る");

    // Act
    Project actual = sut.register(request);

    // Assert
    assertThat(actual.getTitle()).isEqualTo("Spring Boot学習");
    assertThat(actual.getDescription()).isEqualTo("REST APIを作る");
    ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
    verify(repository, times(1)).insert(captor.capture());
    assertThat(captor.getValue()).isSameAs(actual);
  }

  @Test
  void 登録失敗_DB制約違反の例外をそのまま送出すること() {
    // Arrange
    ProjectCreateRequest request = new ProjectCreateRequest();
    request.setTitle(null);
    request.setDescription("説明");

    doThrow(new DataIntegrityViolationException("db constraint violation"))
        .when(repository).insert(any(Project.class));

    // Act & Assert
    assertThatThrownBy(() -> sut.register(request))
        .isInstanceOf(DataIntegrityViolationException.class);

    verify(repository, times(1)).insert(any(Project.class));
  }

  @Test
  void 更新成功_既存プロジェクトを更新できること() {
    // Arrange
    int id = 1;
    ProjectUpdateRequest request = new ProjectUpdateRequest();
    request.setTitle("タスク管理アプリ開発");
    request.setDescription("A社から受託した開発");
    request.setIsFinished(true);

    when(repository.update(any(Project.class))).thenReturn(1);

    // Act
    sut.update(id, request);

    // Assert
    ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
    verify(repository, times(1)).update(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(id);
    assertThat(captor.getValue().getTitle()).isEqualTo("タスク管理アプリ開発");
    assertThat(captor.getValue().getIsFinished()).isTrue();
  }

  @Test
  void 更新失敗_DB制約違反の例外をそのまま送出すること() {
    // Arrange
    int id = 1;
    ProjectUpdateRequest request = new ProjectUpdateRequest();
    request.setTitle(null);
    request.setDescription("説明更新");
    request.setIsFinished(true);

    when(repository.update(any(Project.class)))
        .thenThrow(new DataIntegrityViolationException("db constraint violation"));

    // Act & Assert
    assertThatThrownBy(() -> sut.update(id, request))
        .isInstanceOf(DataIntegrityViolationException.class);

    verify(repository, times(1)).update(any(Project.class));
  }

  @Test
  void 更新失敗_更新件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    int id = 999;
    ProjectUpdateRequest request = new ProjectUpdateRequest();
    request.setTitle("更新されないタイトル");
    request.setDescription("更新されない説明");
    request.setIsFinished(true);

    when(repository.update(any(Project.class))).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.update(id, request))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).update(any(Project.class));
  }


}
