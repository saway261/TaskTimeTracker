package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.entity.TaskGroup;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.repository.ProjectRepository;
import com.kiborisaway.tasktimetracker.repository.TaskGroupRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class TaskGroupServiceTest {

  @Mock
  private TaskGroupRepository tgRepository;

  @Mock
  private ProjectRepository prRepository;

  @InjectMocks
  private TaskGroupService sut;

  @Test
  void タスクグループ一覧検索成功_第2引数にnullを指定するとプロジェクト内全件検索用のリポジトリのメソッドを呼び出すこと() {
    // Arrange
    int pId = 1;
    TaskGroup tg1 = new TaskGroup(1, pId, "タスクグループ１", "説明", false);
    TaskGroup tg2 = new TaskGroup(2, pId, "タスクグループ２", null, true);

    List<TaskGroup> expected = List.of(tg1, tg2);

    when(prRepository.existsById(pId)).thenReturn(true);
    when(tgRepository.findAllInProject(pId)).thenReturn(expected);

    // Act
    List<TaskGroup> actual = sut.findAllByCondition(pId, null);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(prRepository, times(1)).existsById(pId);
    verify(tgRepository, times(1)).findAllInProject(pId);
    verify(tgRepository, never()).findAllInProjectByIsFinished(anyInt(), anyBoolean());
  }

  @ParameterizedTest(name = "[{index}]タスクグループ一覧検索_第2引数に{0}を指定すると完了フラグ指定検索用のリポジトリのメソッドに{0}を指定して呼び出すこと")
  @ValueSource(booleans = {true, false})
  void タスクグループ一覧検索失敗_第2引数に渡したbool値をそのまま完了フラグ指定検索用のリポジトリのメソッドに渡して呼び出すこと(
      boolean flg) {
    // Arrange
    int pId = 1;
    TaskGroup tg = new TaskGroup(2, pId, "タスクグループ２", null, true);

    List<TaskGroup> expected = List.of(tg);

    when(prRepository.existsById(pId)).thenReturn(true);
    when(tgRepository.findAllInProjectByIsFinished(pId, flg)).thenReturn(expected);

    // Act
    List<TaskGroup> actual = sut.findAllByCondition(pId, flg);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(prRepository, times(1)).existsById(pId);
    verify(tgRepository, times(1)).findAllInProjectByIsFinished(pId, flg);
    verify(tgRepository, never()).findAllInProject(anyInt());
  }

  @Test
  void タスクグループ一覧検索失敗_指定したプロジェクトのIDが存在しない場合は例外を投げてその後のリポジトリの処理を呼び出さないこと() {
    // Arrange
    int pId = 999;
    when(prRepository.existsById(pId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.findAllByCondition(pId, null))
        .isInstanceOf(TargetNotFoundException.class);
    verify(prRepository, times(1)).existsById(pId);
    verify(tgRepository, never()).findAllInProject(anyInt());
    verify(tgRepository, never()).findAllInProjectByIsFinished(anyInt(), anyBoolean());
  }

  @Test
  void ID検索成功_タスクグループを取得できること() {
    // Arrange
    int id = 1;
    TaskGroup expected = new TaskGroup(id, 1, "タスクグループ１", "説明", false);

    when(tgRepository.findById(id)).thenReturn(expected);

    // Act
    TaskGroup actual = sut.findById(id);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(tgRepository, times(1)).findById(id);
  }

  @Test
  void ID検索失敗_リポジトリからnullが返ったら例外を投げること() {
    // Arrange
    int id = 999;

    when(tgRepository.findById(id)).thenReturn(null);

    // Assert
    assertThatThrownBy(() -> sut.findById(id))
        .isInstanceOf(TargetNotFoundException.class);
  }

  @Test
  void 登録成功_引数のタスクグループインスタンスにプロジェクトIDをセットしてリポジトリの処理を呼び出すこと() {
    // Arrange
    int pId = 2;
    // ※サービスではINSERT時にidが自動採番されて引数インスタンスに自動でバインドされる挙動を再現できない
    //   かつ、その挙動はリポジトリのテストで検証するので、サービス層のテストでは初めからidを持っておく
    TaskGroup raw = new TaskGroup(2, null, "タスクグループ２", null, true);
    when(prRepository.existsById(pId)).thenReturn(true);
    TaskGroup processed = new TaskGroup(2, pId, "タスクグループ２", null, true);

    // Act
    TaskGroup actual = sut.register(pId, raw);

    // Assert
    assertThat(actual).isEqualTo(processed);
    verify(prRepository, times(1)).existsById(pId);
    verify(tgRepository, times(1)).insert(processed);
  }

  @Test
  void 登録失敗_指定したプロジェクトが存在しない場合は例外を投げて以降のリポジトリの処理を呼び出さないこと() {
    // Arrange
    int pId = 999;
    TaskGroup tg = new TaskGroup();
    tg.setTitle("タイトル");
    tg.setDescription("説明");
    when(prRepository.existsById(pId)).thenReturn(false);

    // Act & Assert
    assertThatThrownBy(() -> sut.register(pId, tg))
        .isInstanceOf(TargetNotFoundException.class);
    verify(prRepository, times(1)).existsById(pId);
    verify(tgRepository, never()).insert(any());
  }

  @Test
  void 登録失敗_DB制約違反の例外をそのまま送出すること() {
    // Arrange
    int pId = 1;
    TaskGroup tg = new TaskGroup();
    tg.setProjectId(pId);
    tg.setTitle(null);
    tg.setDescription("説明");

    when(prRepository.existsById(pId)).thenReturn(true);
    doThrow(new DataIntegrityViolationException("db constraint violation"))
        .when(tgRepository).insert(tg);

    // Act & Assert
    assertThatThrownBy(() -> sut.register(pId, tg))
        .isInstanceOf(DataIntegrityViolationException.class);

    verify(prRepository, times(1)).existsById(pId);
    verify(tgRepository, times(1)).insert(tg);
  }

  @Test
  void 更新成功_リポジトリのメソッドを呼び出すこと() {
    // Arrange
    TaskGroup tg = new TaskGroup(1, 1, "タスクグループ１", "説明", false);

    when(tgRepository.update(tg)).thenReturn(1);

    // Act
    sut.update(tg);

    // Assert
    verify(tgRepository, times(1)).update(same(tg));
  }

  @Test
  void 更新失敗_DB制約違反の例外をそのまま送出すること() {
    // Arrange
    TaskGroup project = new TaskGroup(1, 1, "タスクグループ１", "説明更新", false);

    when(tgRepository.update(project))
        .thenThrow(new DataIntegrityViolationException("db constraint violation"));

    // Act & Assert
    assertThatThrownBy(() -> sut.update(project))
        .isInstanceOf(DataIntegrityViolationException.class);

    verify(tgRepository, times(1)).update(same(project));
  }

  @Test
  void 更新失敗_更新件数が0件のときTargetNotFoundExceptionを投げること() {
    // Arrange
    TaskGroup project = new TaskGroup(999, 1, "タスクグループ１", "説明", false);

    when(tgRepository.update(project)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.update(project))
        .isInstanceOf(TargetNotFoundException.class);

    verify(tgRepository, times(1)).update(same(project));
  }

}