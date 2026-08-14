package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.dto.project.ProjectCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.project.ProjectResponse;
import com.kiborisaway.tasktimetracker.data.dto.project.ProjectUpdateRequest;
import com.kiborisaway.tasktimetracker.data.entity.Project;
import com.kiborisaway.tasktimetracker.data.entity.Memo;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.MemoRepository;
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

  private static final int USER_ID = 1;

  @Mock
  private ProjectRepository repository;

  @Mock
  private MemoRepository memoRepository;

  @InjectMocks
  private ProjectService sut;

  @Test
  void プロジェクト一覧検索_引数にnullを指定すると全件検索用のリポジトリのメソッドを呼び出すこと() {
    // Arrange
    Project project1 = new Project(1, "タスク管理アプリ開発", "A社から受託した開発", false);
    Project project2 = new Project(2, "Java Silver勉強", null, true);

    List<Project> expected = List.of(project1, project2);

    when(repository.findAllByUserId(USER_ID)).thenReturn(expected);
    when(memoRepository.findAllInProjects(List.of(1, 2))).thenReturn(List.of(
        new Memo(1, 1, null, null, "プロジェクト1メモ"),
        new Memo(2, 2, null, null, "プロジェクト2メモ")));

    // Act
    List<ProjectResponse> actual = sut.findAllByCondition(USER_ID, null);

    // Assert
    assertThat(actual)
        .extracting(ProjectResponse::getId, ProjectResponse::getTitle, ProjectResponse::getDescription,
            ProjectResponse::getIsFinished)
        .containsExactly(
            org.assertj.core.api.Assertions.tuple(1, "タスク管理アプリ開発", "A社から受託した開発", false),
            org.assertj.core.api.Assertions.tuple(2, "Java Silver勉強", null, true)
        );
    verify(repository, times(1)).findAllByUserId(USER_ID);
    verify(memoRepository, times(1)).findAllInProjects(List.of(1, 2));
    verify(memoRepository, never()).findAllInProject(org.mockito.ArgumentMatchers.anyInt());
    assertThat(actual).allSatisfy(response -> assertThat(response.getMemos()).hasSize(1));
  }

  @Test
  void プロジェクト一覧検索_引数にtrueを指定すると完了フラグ指定検索用のリポジトリのメソッドにtrueを指定して呼び出すこと() {
    // Arrange
    Project project = new Project(2, "Java Silver勉強", null, true);

    List<Project> expected = List.of(project);

    when(repository.findAllByIsFinishedAndUserId(true, USER_ID)).thenReturn(expected);

    // Act
    List<ProjectResponse> actual = sut.findAllByCondition(USER_ID, true);

    // Assert
    assertThat(actual)
        .extracting(ProjectResponse::getId, ProjectResponse::getTitle, ProjectResponse::getIsFinished)
        .containsExactly(org.assertj.core.api.Assertions.tuple(2, "Java Silver勉強", true));
    verify(repository, times(1)).findAllByIsFinishedAndUserId(true, USER_ID);
  }

  @Test
  void プロジェクト一覧検索_検索結果が空ならメモ検索を実行しないこと() {
    when(repository.findAllByUserId(USER_ID)).thenReturn(List.of());

    List<ProjectResponse> actual = sut.findAllByCondition(USER_ID, null);

    assertThat(actual).isEmpty();
    verify(memoRepository, never()).findAllInProjects(any());
  }

  @Test
  void プロジェクト一覧検索_引数にfalseを指定すると完了フラグ指定検索用のリポジトリのメソッドにfalseを指定して呼び出すこと() {
    // Arrange
    Project project = new Project(1, "タスク管理アプリ開発", "A社から受託した開発", false);

    List<Project> expected = List.of(project);

    when(repository.findAllByIsFinishedAndUserId(false, USER_ID)).thenReturn(expected);

    // Act
    List<ProjectResponse> actual = sut.findAllByCondition(USER_ID, false);

    // Assert
    assertThat(actual)
        .extracting(ProjectResponse::getId, ProjectResponse::getTitle, ProjectResponse::getIsFinished)
        .containsExactly(org.assertj.core.api.Assertions.tuple(1, "タスク管理アプリ開発", false));
    verify(repository, times(1)).findAllByIsFinishedAndUserId(false, USER_ID);
  }

  @Test
  void ID検索成功_プロジェクトを取得できること() {
    // Arrange
    int id = 1;
    Project expected = new Project(id, "タスク管理アプリ開発", "A社から受託した開発", false);

    when(repository.findByIdAndUserId(id, USER_ID)).thenReturn(expected);

    // Act
    ProjectResponse actual = sut.findById(USER_ID, id);

    // Assert
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getTitle()).isEqualTo(expected.getTitle());
    assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
    assertThat(actual.getIsFinished()).isEqualTo(expected.getIsFinished());
    assertThat(actual.getMemos()).isEmpty();
    verify(repository, times(1)).findByIdAndUserId(id, USER_ID);
  }

  @Test
  void ID検索失敗_リポジトリからnullが返ったら例外を投げること() {
    // Arrange
    int id = 999;

    when(repository.findByIdAndUserId(id, USER_ID)).thenReturn(null);

    // Assert
    assertThatThrownBy(() -> sut.findById(USER_ID, id))
        .isInstanceOf(TargetNotFoundException.class);
  }

  @Test
  void 登録成功_リクエストの内容でプロジェクトを登録し登録したインスタンスを返すこと() {
    // Arrange
    ProjectCreateRequest request = new ProjectCreateRequest();
    request.setTitle("Spring Boot学習");
    request.setDescription("REST APIを作る");
    doAnswer(invocation -> {
      Project project = invocation.getArgument(0);
      project.setId(10);
      return null;
    }).when(repository).insert(any(Project.class));
    Project registered = new Project(10, "Spring Boot学習", "REST APIを作る", false);
    when(repository.findByIdAndUserId(10, USER_ID)).thenReturn(registered);

    // Act
    ProjectResponse actual = sut.register(USER_ID, request);

    // Assert
    assertThat(actual.getTitle()).isEqualTo("Spring Boot学習");
    assertThat(actual.getDescription()).isEqualTo("REST APIを作る");
    assertThat(actual.getIsFinished()).isFalse();
    assertThat(actual.getMemos()).isEmpty();
    ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
    verify(repository, times(1)).insert(captor.capture());
    assertThat(captor.getValue().getTitle()).isEqualTo("Spring Boot学習");
    assertThat(captor.getValue().getDescription()).isEqualTo("REST APIを作る");
    assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
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
    assertThatThrownBy(() -> sut.register(USER_ID, request))
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
    Project updated = new Project(id, "DB更新後タイトル", "DB更新後説明", true);
    when(repository.findByIdAndUserId(id, USER_ID)).thenReturn(updated);

    // Act
    ProjectResponse actual = sut.update(USER_ID, id, request);

    // Assert
    ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
    verify(repository, times(1)).update(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(id);
    assertThat(captor.getValue().getTitle()).isEqualTo("タスク管理アプリ開発");
    assertThat(captor.getValue().getIsFinished()).isTrue();
    assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    assertThat(actual.getTitle()).isEqualTo("DB更新後タイトル");
    assertThat(actual.getDescription()).isEqualTo("DB更新後説明");
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
    assertThatThrownBy(() -> sut.update(USER_ID, id, request))
        .isInstanceOf(DataIntegrityViolationException.class);

    verify(repository, times(1)).update(any(Project.class));
    verify(repository, never()).findByIdAndUserId(id, USER_ID);
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
    assertThatThrownBy(() -> sut.update(USER_ID, id, request))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).update(any(Project.class));
  }


}
