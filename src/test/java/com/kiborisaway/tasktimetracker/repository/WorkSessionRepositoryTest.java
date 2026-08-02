package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.kiborisaway.tasktimetracker.data.entity.WorkSession;
import com.kiborisaway.tasktimetracker.data.entity.WorkSessionType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;

@MybatisTest
class WorkSessionRepositoryTest {

  @Autowired
  private WorkSessionRepository sut;

  @Test
  void 合計取得_指定したタスクIDに紐づく作業セッションのminutes合計を取得できること() {
    // Act
    int actual = sut.sumMinutesByTaskId(4);

    // Assert
    assertThat(actual).isEqualTo(75);
  }

  @Test
  void 合計取得_指定したタスクIDに紐づくminutesがnullのみの場合は0を返すこと() {
    // Act
    int actual = sut.sumMinutesByTaskId(1);

    // Assert
    assertThat(actual).isZero();
  }

  @Test
  void 合計取得_指定したタスクグループ配下の作業セッションのminutes合計を取得できること() {
    // Arrange
    WorkSession workSession = new WorkSession();
    workSession.setTaskId(1);
    workSession.setMinutes(25);
    workSession.setType(WorkSessionType.MANUAL);
    sut.insert(workSession);

    // Act
    int actual = sut.sumMinutesByTaskGroupId(1);

    // Assert
    assertThat(actual).isEqualTo(25);
  }

  @Test
  void 合計取得_指定したプロジェクト配下の直下タスクとタスクグループ配下タスクのminutes合計を取得できること() {
    // Arrange
    WorkSession workSession = new WorkSession();
    workSession.setTaskId(1);
    workSession.setMinutes(25);
    workSession.setType(WorkSessionType.MANUAL);
    sut.insert(workSession);

    // Act
    int actual = sut.sumMinutesByProjectId(1);

    // Assert
    assertThat(actual).isEqualTo(100);
  }

  @Test
  void 一覧取得_指定したタスクIDに紐づく作業セッション一覧を取得できること() {
    // Act
    List<WorkSession> actual = sut.findAllByTaskId(4);

    // Assert
    assertThat(actual)
        .extracting(WorkSession::getTaskId, WorkSession::getMinutes, WorkSession::getType)
        .containsExactly(
            tuple(4, 30, WorkSessionType.TIMER),
            tuple(4, 45, WorkSessionType.TIMER)
        );
  }

  @Test
  void 一覧取得_存在しないタスクIDを指定すると空のリストを返すこと() {
    // Act
    List<WorkSession> actual = sut.findAllByTaskId(999);

    // Assert
    assertThat(actual).isEmpty();
  }

  @Test
  void ID検索成功_IDが一致する作業セッションを取得できること() {
    // Act
    WorkSession actual = sut.findById(2);

    // Assert
    assertThat(actual.getId()).isEqualTo(2);
    assertThat(actual.getTaskId()).isEqualTo(4);
    assertThat(actual.getMinutes()).isEqualTo(30);
    assertThat(actual.getStartedAt()).isEqualTo(LocalDateTime.of(2026, 1, 2, 9, 0));
    assertThat(actual.getEndedAt()).isEqualTo(LocalDateTime.of(2026, 1, 2, 9, 30));
    assertThat(actual.getType()).isEqualTo(WorkSessionType.TIMER);
  }

  @Test
  void ID検索失敗_存在しないIDを指定するとnullを返すこと() {
    // Act
    WorkSession actual = sut.findById(999);

    // Assert
    assertThat(actual).isNull();
  }

  @Test
  void タスクID取得成功_作業セッションIDに紐づくタスクIDを取得できること() {
    // Act
    Integer actual = sut.findTaskIdById(2);

    // Assert
    assertThat(actual).isEqualTo(4);
  }

  @Test
  void タスクID取得失敗_存在しない作業セッションIDならnullを返すこと() {
    // Act
    Integer actual = sut.findTaskIdById(999);

    // Assert
    assertThat(actual).isNull();
  }

  @Test
  void タスクの作業セッション存在チェック_指定したタスクIDに対して作業セッションが存在するならtrueを返すこと() {
    // Arrange
    int taskId = 1;

    // Act
    boolean exists = sut.existsByTaskId(taskId);

    // Assert
    assertThat(exists).isTrue();
  }

  @Test
  void タスクの作業セッション存在チェック_指定したタスクIDに対して作業セッションが存在しないならfalseを返すこと() {
    // Arrange
    int taskId = 2;

    // Act
    boolean exists = sut.existsByTaskId(taskId);

    // Assert
    assertThat(exists).isFalse();
  }

  @Test
  void 未終了作業セッション存在チェック_minutesがnullの作業セッションがあるならtrueを返すこと() {
    // Arrange
    int taskId = 1;

    // Act
    boolean actual = sut.existsUnfinishedByTaskId(taskId);

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  void 未終了作業セッション存在チェック_minutesがnullの作業セッションがないならfalseを返すこと() {
    // Arrange
    int taskId = 4;

    // Act
    boolean actual = sut.existsUnfinishedByTaskId(taskId);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  void 登録成功_TIMERの作業セッションを登録でき自動採番と日時が設定されること() {
    // Arrange
    WorkSession workSession = new WorkSession();
    workSession.setTaskId(2);
    workSession.setType(WorkSessionType.TIMER);

    // Act
    sut.insert(workSession);

    // Assert
    assertThat(workSession.getId()).isNotNull();
    WorkSession registered = sut.findById(workSession.getId());
    assertThat(registered.getTaskId()).isEqualTo(2);
    assertThat(registered.getMinutes()).isNull();
    assertThat(registered.getStartedAt()).isNotNull();
    assertThat(registered.getEndedAt()).isNull();
    assertThat(registered.getCreatedAt()).isNotNull();
    assertThat(registered.getUpdatedAt()).isNotNull();
    assertThat(registered.getType()).isEqualTo(WorkSessionType.TIMER);
  }

  @Test
  void 登録成功_MANUALの作業セッションを登録できstartedAtはnullになること() {
    // Arrange
    WorkSession workSession = new WorkSession();
    workSession.setTaskId(2);
    workSession.setMinutes(40);
    workSession.setType(WorkSessionType.MANUAL);

    // Act
    sut.insert(workSession);

    // Assert
    assertThat(workSession.getId()).isNotNull();
    WorkSession registered = sut.findById(workSession.getId());
    assertThat(registered.getTaskId()).isEqualTo(2);
    assertThat(registered.getMinutes()).isEqualTo(40);
    assertThat(registered.getStartedAt()).isNull();
    assertThat(registered.getEndedAt()).isNull();
    assertThat(registered.getCreatedAt()).isNotNull();
    assertThat(registered.getUpdatedAt()).isNotNull();
    assertThat(registered.getType()).isEqualTo(WorkSessionType.MANUAL);
  }

  @Test
  void 終了可否判定_TIMERかつstartedAtがある作業セッションならtrueを返すこと() {
    // Act
    boolean actual = sut.canSetEnd(1);

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  void 終了可否判定_存在しない作業セッションならfalseを返すこと() {
    // Act
    boolean actual = sut.canSetEnd(999);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  void 終了更新成功_指定した作業セッションにendedAtとupdatedAtを設定できること() {
    // Arrange
    WorkSession before = sut.findById(1);

    // Act
    int actual = sut.setEnd(1);

    // Assert
    assertThat(actual).isEqualTo(1);
    WorkSession updated = sut.findById(1);
    assertThat(updated.getEndedAt()).isNotNull();
    assertThat(updated.getMinutes())
        .isEqualTo(
            (int) Duration.between(updated.getStartedAt(), updated.getEndedAt()).toMinutes());
    assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(before.getUpdatedAt());
    assertThat(updated.getTaskId()).isEqualTo(before.getTaskId());
    assertThat(updated.getType()).isEqualTo(before.getType());
  }

  @Test
  void 終了更新失敗_存在しない作業セッションIDの場合は更新されず0件となること() {
    // Act
    int actual = sut.setEnd(999);

    // Assert
    assertThat(actual).isZero();
  }

  @Test
  void 更新成功_startedAtとendedAtを指定すると差分からminutesを計算して更新できること() {
    // Arrange
    WorkSession before = sut.findById(2);
    WorkSession workSession = new WorkSession();
    workSession.setId(2);
    workSession.setTaskId(1);
    workSession.setMinutes(999);
    workSession.setStartedAt(LocalDateTime.of(2026, 1, 3, 9, 0));
    workSession.setEndedAt(LocalDateTime.of(2026, 1, 3, 10, 15));
    workSession.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
    workSession.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
    workSession.setType(WorkSessionType.MANUAL);

    // Act
    int actual = sut.update(workSession);

    // Assert
    assertThat(actual).isEqualTo(1);
    WorkSession updated = sut.findById(2);
    assertThat(updated.getMinutes()).isEqualTo(75);
    assertThat(updated.getStartedAt()).isEqualTo(LocalDateTime.of(2026, 1, 3, 9, 0));
    assertThat(updated.getEndedAt()).isEqualTo(LocalDateTime.of(2026, 1, 3, 10, 15));
    assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(before.getUpdatedAt());
    assertThat(updated.getTaskId()).isEqualTo(before.getTaskId());
    assertThat(updated.getCreatedAt()).isEqualTo(before.getCreatedAt());
    assertThat(updated.getType()).isEqualTo(before.getType());
  }

  @Test
  void 更新成功_minutesのみを指定すると受け取ったminutesを更新し日時はnullにできること() {
    // Arrange
    WorkSession before = sut.findById(2);
    WorkSession workSession = new WorkSession();
    workSession.setId(2);
    workSession.setTaskId(1);
    workSession.setMinutes(60);
    workSession.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
    workSession.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
    workSession.setType(WorkSessionType.MANUAL);

    // Act
    int actual = sut.update(workSession);

    // Assert
    assertThat(actual).isEqualTo(1);
    WorkSession updated = sut.findById(2);
    assertThat(updated.getMinutes()).isEqualTo(60);
    assertThat(updated.getStartedAt()).isNull();
    assertThat(updated.getEndedAt()).isNull();
    assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(before.getUpdatedAt());
    assertThat(updated.getTaskId()).isEqualTo(before.getTaskId());
    assertThat(updated.getCreatedAt()).isEqualTo(before.getCreatedAt());
    assertThat(updated.getType()).isEqualTo(before.getType());
  }

  @Test
  void 更新失敗_存在しない作業セッションIDの場合は更新されず0件となること() {
    // Arrange
    WorkSession workSession = new WorkSession();
    workSession.setId(999);
    workSession.setMinutes(60);
    workSession.setStartedAt(LocalDateTime.of(2026, 1, 3, 9, 0));
    workSession.setEndedAt(LocalDateTime.of(2026, 1, 3, 10, 0));

    // Act
    int actual = sut.update(workSession);

    // Assert
    assertThat(actual).isZero();
  }

  @Test
  void 削除成功_指定した作業セッションを削除できること() {
    // Arrange
    List<WorkSession> before = sut.findAllByTaskId(4);

    // Act
    int actual = sut.deleteById(2);

    // Assert
    assertThat(actual).isEqualTo(1);
    assertThat(sut.findById(2)).isNull();
    assertThat(sut.findAllByTaskId(4)).hasSize(before.size() - 1);
  }

  @Test
  void 削除失敗_存在しない作業セッションIDの場合は削除されず0件となること() {
    // Act
    int actual = sut.deleteById(999);

    // Assert
    assertThat(actual).isZero();
  }

}
