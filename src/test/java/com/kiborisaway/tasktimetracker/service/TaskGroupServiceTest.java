package com.kiborisaway.tasktimetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kiborisaway.tasktimetracker.data.TaskGroup;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
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
  private TaskGroupRepository repository;

  @InjectMocks
  private TaskGroupService sut;

  @Test
  void タスクグループ一覧検索_第2引数にnullを指定するとプロジェクト内全件検索用のリポジトリのメソッドを呼び出すこと() {
    // Arrange
    int pId = 1;
    TaskGroup tg1 = new TaskGroup(1, pId, "タスクグループ１", "説明", false);
    TaskGroup tg2 = new TaskGroup(2, pId, "タスクグループ２", null, true);

    List<TaskGroup> expected = List.of(tg1, tg2);

    when(repository.findAllInProject(pId)).thenReturn(expected);

    // Act
    List<TaskGroup> actual = sut.findAllByCondition(pId, null);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(repository, times(1)).findAllInProject(pId);
  }

  @ParameterizedTest(name = "[{index}]タスクグループ一覧検索_第2引数に{0}を指定すると完了フラグ指定検索用のリポジトリのメソッドに{0}を指定して呼び出すこと")
  @ValueSource(booleans = {true, false})
  void タスクグループ一覧検索_第2引数に渡したbool値をそのまま完了フラグ指定検索用のリポジトリのメソッドに渡して呼び出すこと(
      boolean flg) {
    // Arrange
    int pId = 1;
    TaskGroup tg = new TaskGroup(2, pId, "タスクグループ２", null, true);

    List<TaskGroup> expected = List.of(tg);

    when(repository.findAllInProjectByIsFinished(pId, flg)).thenReturn(expected);

    // Act
    List<TaskGroup> actual = sut.findAllByCondition(pId, flg);

    // Assert
    assertThat(actual).isEqualTo(expected);
    verify(repository, times(1)).findAllInProjectByIsFinished(pId, flg);
  }

  @Test
  void ID検索成功_タスクグループを取得できること() {
    // Arrange
    int id = 1;
    TaskGroup expected = new TaskGroup(id, 1, "タスクグループ１", "説明", false);

    when(repository.findById(id)).thenReturn(expected);

    // Act
    TaskGroup actual = sut.findById(id);

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
  void 登録成功_リポジトリのメソッドを呼び出し引数と同じ同一インスタンスを返すこと() {
    // Arrange
    // ※サービスではINSERT時にidが自動採番されて引数インスタンスに自動でバインドされる挙動を再現できない
    //   かつ、その挙動はリポジトリのテストで検証するので、サービス層のテストでは初めからidを持っておく
    TaskGroup tg = new TaskGroup(2, 2, "タスクグループ２", null, true);

    // Act
    TaskGroup actual = sut.register(tg);

    // Assert
    assertThat(actual).isSameAs(tg);
    verify(repository, times(1)).insert(same(tg));
  }

  @Test
  void 登録失敗_DB制約違反の例外をそのまま送出すること() {
    // Arrange
    TaskGroup tg = new TaskGroup();
    tg.setTitle(null);
    tg.setDescription("説明");

    doThrow(new DataIntegrityViolationException("db constraint violation"))
        .when(repository).insert(same(tg));

    // Act & Assert
    assertThatThrownBy(() -> sut.register(tg))
        .isInstanceOf(DataIntegrityViolationException.class);

    verify(repository, times(1)).insert(same(tg));
  }

  @Test
  void 更新成功_リポジトリのメソッドを呼び出すこと() {
    // Arrange
    TaskGroup tg = new TaskGroup(1, 1, "タスクグループ１", "説明", false);

    when(repository.update(tg)).thenReturn(1);

    // Act
    sut.update(tg);

    // Assert
    verify(repository, times(1)).update(same(tg));
  }

  @Test
  void 更新失敗_DB制約違反の例外をそのまま送出すること() {
    // Arrange
    TaskGroup project = new TaskGroup(1, 1, "タスクグループ１", "説明更新", false);

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
    TaskGroup project = new TaskGroup(999, 1, "タスクグループ１", "説明", false);

    when(repository.update(project)).thenReturn(0);

    // Act & Assert
    assertThatThrownBy(() -> sut.update(project))
        .isInstanceOf(TargetNotFoundException.class);

    verify(repository, times(1)).update(same(project));
  }


}