package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.kiborisaway.tasktimetracker.data.dto.work_session.ActiveTimerResponse;
import com.kiborisaway.tasktimetracker.data.entity.WorkSession;
import com.kiborisaway.tasktimetracker.data.entity.WorkSessionType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@MybatisTest
class WorkSessionRepositoryTest {

  private static final int USER_A = 1;
  private static final int USER_B = 2;

  @Autowired
  private WorkSessionRepository sut;

  @Test
  void 稼働中タイマー一覧取得_所有する全タスクの未終了タイマーと親情報を取得できること() {
    // Arrange: データセットにはタスク1の稼働中タイマーがあり、別の直下タスクにも追加する。
    WorkSession another = new WorkSession();
    another.setTaskId(4);
    another.setType(WorkSessionType.TIMER);
    sut.insert(another);

    // Act
    List<ActiveTimerResponse> actual = sut.findAllActiveByUserId(USER_A);

    // Assert
    assertThat(actual)
        .extracting(
            ActiveTimerResponse::getTaskId,
            ActiveTimerResponse::getTaskTitle,
            ActiveTimerResponse::getProjectId,
            ActiveTimerResponse::getTaskGroupId)
        .containsExactly(
            tuple(1, "カスタム例外作成", 1, 1),
            tuple(4, "画面設計", 1, null));
    assertThat(actual).allSatisfy(timer -> {
      assertThat(timer.getSessionId()).isPositive();
      assertThat(timer.getStartedAt()).isNotNull();
    });
  }

  @Test
  void 稼働中タイマー一覧取得_他ユーザーのタイマーを返さないこと() {
    assertThat(sut.findAllActiveByUserId(USER_B)).isEmpty();
  }

  @Test
  void 合計取得_指定したタスクIDに紐づく作業セッションの合計分を取得できること() {
    // Act
    int actual = sut.sumMinutesByTaskId(4, USER_A);

    // Assert
    assertThat(actual).isEqualTo(75);
  }

  @Test
  void 合計取得_各セッションでは切り捨てられる秒数も合計してから分へ切り捨てること() {
    // Arrange
    WorkSession first = insertTimerSession(2, 30);
    WorkSession second = insertTimerSession(2, 30);

    // Act
    int actual = sut.sumMinutesByTaskId(2, USER_A);

    // Assert
    assertThat(first.getDurationSeconds()).isEqualTo(30);
    assertThat(first.getMinutes()).isZero();
    assertThat(second.getDurationSeconds()).isEqualTo(30);
    assertThat(second.getMinutes()).isZero();
    assertThat(actual).isEqualTo(1);
  }

  @Test
  void 合計取得_所有者が一致しない場合は0を返すこと() {
    // Act
    int actual = sut.sumMinutesByTaskId(4, USER_B);

    // Assert
    assertThat(actual).isZero();
  }

  @Test
  void 合計取得_指定したタスクIDに紐づくdurationSecondsがnullのみの場合は0を返すこと() {
    // Act
    int actual = sut.sumMinutesByTaskId(1, USER_A);

    // Assert
    assertThat(actual).isZero();
  }

  @Test
  void 合計取得_指定したタスクグループ配下の作業セッションの合計分を取得できること() {
    // Arrange
    WorkSession workSession = new WorkSession();
    workSession.setTaskId(1);
    workSession.setMinutes(25);
    workSession.setType(WorkSessionType.MANUAL);
    sut.insert(workSession);

    // Act
    int actual = sut.sumMinutesByTaskGroupId(1, USER_A);

    // Assert
    assertThat(actual).isEqualTo(25);
  }

  @Test
  void 合計取得_タスクグループ内の異なるタスクの秒数を合計してから分へ切り捨てること() {
    // Arrange
    insertTimerSession(1, 30);
    insertTimerSession(2, 30);

    // Act & Assert
    assertThat(sut.sumMinutesByTaskGroupId(1, USER_A)).isEqualTo(1);
  }

  @Test
  void 合計取得_指定したプロジェクト配下の直下タスクとタスクグループ配下タスクの合計分を取得できること() {
    // Arrange
    WorkSession workSession = new WorkSession();
    workSession.setTaskId(1);
    workSession.setMinutes(25);
    workSession.setType(WorkSessionType.MANUAL);
    sut.insert(workSession);

    // Act
    int actual = sut.sumMinutesByProjectId(1, USER_A);

    // Assert
    assertThat(actual).isEqualTo(100);
  }

  @Test
  void 合計取得_プロジェクト内の異なるタスクの秒数を合計してから分へ切り捨てること() {
    // Arrange
    insertTimerSession(1, 30);
    insertTimerSession(2, 30);

    // Act & Assert
    assertThat(sut.sumMinutesByProjectId(1, USER_A)).isEqualTo(76);
  }

  @Test
  void 一覧取得_指定したタスクIDに紐づく作業セッション一覧を取得できること() {
    // Act
    List<WorkSession> actual = sut.findAllByTaskId(4, USER_A);

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
    List<WorkSession> actual = sut.findAllByTaskId(999, USER_A);

    // Assert
    assertThat(actual).isEmpty();
  }

  @Test
  void 一覧取得_所有者が一致しない場合は空のリストを返すこと() {
    // Act
    List<WorkSession> actual = sut.findAllByTaskId(4, USER_B);

    // Assert
    assertThat(actual).isEmpty();
  }

  @Test
  void ID検索成功_IDが一致する作業セッションを取得できること() {
    // Act
    WorkSession actual = sut.findById(2, USER_A);

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
    WorkSession actual = sut.findById(999, USER_A);

    // Assert
    assertThat(actual).isNull();
  }

  @Test
  void ID検索失敗_所有者が一致しない場合はnullを返すこと() {
    // Act
    WorkSession actual = sut.findById(2, USER_B);

    // Assert
    assertThat(actual).isNull();
  }

  @Test
  void タスクID取得成功_作業セッションIDに紐づくタスクIDを取得できること() {
    // Act
    Integer actual = sut.findTaskIdById(2, USER_A);

    // Assert
    assertThat(actual).isEqualTo(4);
  }

  @Test
  void タスクID取得失敗_存在しない作業セッションIDならnullを返すこと() {
    // Act
    Integer actual = sut.findTaskIdById(999, USER_A);

    // Assert
    assertThat(actual).isNull();
  }

  @Test
  void タスクID取得失敗_所有者が一致しない場合はnullを返すこと() {
    // Act
    Integer actual = sut.findTaskIdById(2, USER_B);

    // Assert
    assertThat(actual).isNull();
  }

  @Test
  void タスクの作業セッション存在チェック_指定したタスクIDに対して作業セッションが存在するならtrueを返すこと() {
    // Arrange
    int taskId = 1;

    // Act
    boolean exists = sut.existsByTaskId(taskId, USER_A);

    // Assert
    assertThat(exists).isTrue();
  }

  @Test
  void タスクの作業セッション存在チェック_指定したタスクIDに対して作業セッションが存在しないならfalseを返すこと() {
    // Arrange
    int taskId = 2;

    // Act
    boolean exists = sut.existsByTaskId(taskId, USER_A);

    // Assert
    assertThat(exists).isFalse();
  }

  @Test
  void タスクの作業セッション存在チェック_所有者が一致しない場合はfalseを返すこと() {
    // Act
    boolean exists = sut.existsByTaskId(1, USER_B);

    // Assert
    assertThat(exists).isFalse();
  }

  @Test
  void 未終了作業セッション存在チェック_endedAtがnullのTIMERがあるならtrueを返すこと() {
    // Arrange
    int taskId = 1;

    // Act
    boolean actual = sut.existsUnfinishedByTaskId(taskId, USER_A);

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  void 未終了作業セッション存在チェック_endedAtがnullのTIMERがないならfalseを返すこと() {
    // Arrange
    int taskId = 4;

    // Act
    boolean actual = sut.existsUnfinishedByTaskId(taskId, USER_A);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  void 未終了作業セッション存在チェック_所有者が一致しない場合はfalseを返すこと() {
    // Act
    boolean actual = sut.existsUnfinishedByTaskId(1, USER_B);

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
    WorkSession registered = sut.findById(workSession.getId(), USER_A);
    assertThat(registered.getTaskId()).isEqualTo(2);
    assertThat(registered.getMinutes()).isNull();
    assertThat(registered.getDurationSeconds()).isNull();
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
    WorkSession registered = sut.findById(workSession.getId(), USER_A);
    assertThat(registered.getTaskId()).isEqualTo(2);
    assertThat(registered.getMinutes()).isEqualTo(40);
    assertThat(registered.getDurationSeconds()).isEqualTo(2400);
    assertThat(registered.getStartedAt()).isNull();
    assertThat(registered.getEndedAt()).isNull();
    assertThat(registered.getCreatedAt()).isNotNull();
    assertThat(registered.getUpdatedAt()).isNotNull();
    assertThat(registered.getType()).isEqualTo(WorkSessionType.MANUAL);
  }

  @Test
  void 終了可否判定_TIMERかつstartedAtがある作業セッションならtrueを返すこと() {
    // Act
    boolean actual = sut.canSetEnd(1, USER_A);

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  void 終了可否判定_終了済みの作業セッションならfalseを返すこと() {
    // Act
    boolean actual = sut.canSetEnd(2, USER_A);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  void 終了可否判定_存在しない作業セッションならfalseを返すこと() {
    // Act
    boolean actual = sut.canSetEnd(999, USER_A);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  void 終了可否判定_所有者が一致しない場合はfalseを返すこと() {
    // Act
    boolean actual = sut.canSetEnd(1, USER_B);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  void 終了更新成功_指定した作業セッションにendedAtとupdatedAtを設定できること() {
    // Arrange
    WorkSession before = sut.findById(1, USER_A);

    // Act
    int actual = sut.setEnd(1, USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);
    WorkSession updated = sut.findById(1, USER_A);
    assertThat(updated.getEndedAt()).isNotNull();
    Duration duration = Duration.between(updated.getStartedAt(), updated.getEndedAt());
    assertThat(updated.getDurationSeconds()).isEqualTo(duration.getSeconds());
    assertThat(updated.getMinutes()).isEqualTo((int) duration.toMinutes());
    assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(before.getUpdatedAt());
    assertThat(updated.getTaskId()).isEqualTo(before.getTaskId());
    assertThat(updated.getType()).isEqualTo(before.getType());
  }

  @Test
  void 終了更新失敗_存在しない作業セッションIDの場合は更新されず0件となること() {
    // Act
    int actual = sut.setEnd(999, USER_A);

    // Assert
    assertThat(actual).isZero();
  }

  @Test
  void 終了更新失敗_終了済みの作業セッションは上書きされず0件となること() {
    // Arrange
    WorkSession before = sut.findById(2, USER_A);

    // Act
    int actual = sut.setEnd(2, USER_A);

    // Assert
    assertThat(actual).isZero();
    WorkSession after = sut.findById(2, USER_A);
    assertThat(after.getEndedAt()).isEqualTo(before.getEndedAt());
    assertThat(after.getMinutes()).isEqualTo(before.getMinutes());
    assertThat(after.getDurationSeconds()).isEqualTo(before.getDurationSeconds());
    assertThat(after.getUpdatedAt()).isEqualTo(before.getUpdatedAt());
  }

  @Test
  void 終了更新失敗_所有者が一致しない場合は更新されず0件となること() {
    // Act
    int actual = sut.setEnd(1, USER_B);

    // Assert
    assertThat(actual).isZero();
  }

  @Test
  void 更新成功_startedAtとendedAtを指定すると差分からdurationSecondsを計算して更新できること() {
    // Arrange
    WorkSession before = sut.findById(2, USER_A);
    WorkSession workSession = new WorkSession();
    workSession.setId(2);
    workSession.setTaskId(1);
    workSession.setMinutes(999);
    workSession.setStartedAt(LocalDateTime.of(2026, 1, 3, 9, 0));
    workSession.setEndedAt(LocalDateTime.of(2026, 1, 3, 10, 15, 30));
    workSession.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
    workSession.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
    workSession.setType(WorkSessionType.MANUAL);

    // Act
    int actual = sut.update(workSession, USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);
    WorkSession updated = sut.findById(2, USER_A);
    assertThat(updated.getMinutes()).isEqualTo(75);
    assertThat(updated.getDurationSeconds()).isEqualTo(4530);
    assertThat(updated.getStartedAt()).isEqualTo(LocalDateTime.of(2026, 1, 3, 9, 0));
    assertThat(updated.getEndedAt()).isEqualTo(LocalDateTime.of(2026, 1, 3, 10, 15, 30));
    assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(before.getUpdatedAt());
    assertThat(updated.getTaskId()).isEqualTo(before.getTaskId());
    assertThat(updated.getCreatedAt()).isEqualTo(before.getCreatedAt());
    assertThat(updated.getType()).isEqualTo(before.getType());
  }

  @Test
  void 更新成功_minutesのみを指定すると受け取ったminutesを更新し日時はnullにできること() {
    // Arrange
    WorkSession manual = new WorkSession();
    manual.setTaskId(2);
    manual.setMinutes(40);
    manual.setType(WorkSessionType.MANUAL);
    sut.insert(manual);
    WorkSession before = sut.findById(manual.getId(), USER_A);
    WorkSession workSession = new WorkSession();
    workSession.setId(manual.getId());
    workSession.setTaskId(2);
    workSession.setMinutes(60);
    workSession.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
    workSession.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
    workSession.setType(WorkSessionType.MANUAL);

    // Act
    int actual = sut.update(workSession, USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);
    WorkSession updated = sut.findById(manual.getId(), USER_A);
    assertThat(updated.getMinutes()).isEqualTo(60);
    assertThat(updated.getDurationSeconds()).isEqualTo(3600);
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
    int actual = sut.update(workSession, USER_A);

    // Assert
    assertThat(actual).isZero();
  }

  @Test
  void 更新失敗_所有者が一致しない場合は更新されず0件となること() {
    // Arrange
    WorkSession workSession = new WorkSession();
    workSession.setId(2);
    workSession.setMinutes(60);
    workSession.setStartedAt(LocalDateTime.of(2026, 1, 3, 9, 0));
    workSession.setEndedAt(LocalDateTime.of(2026, 1, 3, 10, 0));

    // Act
    int actual = sut.update(workSession, USER_B);

    // Assert
    assertThat(actual).isZero();
  }

  @Test
  void 更新失敗_終了日時が開始日時より前ならCHECK制約違反になること() {
    // Arrange
    WorkSession workSession = new WorkSession();
    workSession.setId(2);
    workSession.setMinutes(60);
    workSession.setStartedAt(LocalDateTime.of(2026, 1, 3, 10, 0));
    workSession.setEndedAt(LocalDateTime.of(2026, 1, 3, 9, 0));

    // Act & Assert
    assertThatThrownBy(() -> sut.update(workSession, USER_A))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 削除成功_指定した作業セッションを削除できること() {
    // Arrange
    List<WorkSession> before = sut.findAllByTaskId(4, USER_A);

    // Act
    int actual = sut.deleteById(2, USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);
    assertThat(sut.findById(2, USER_A)).isNull();
    assertThat(sut.findAllByTaskId(4, USER_A)).hasSize(before.size() - 1);
  }

  @Test
  void 削除失敗_存在しない作業セッションIDの場合は削除されず0件となること() {
    // Act
    int actual = sut.deleteById(999, USER_A);

    // Assert
    assertThat(actual).isZero();
  }

  @Test
  void 削除失敗_所有者が一致しない場合は削除されず0件となること() {
    // Act
    int actual = sut.deleteById(2, USER_B);

    // Assert
    assertThat(actual).isZero();
    assertThat(sut.findById(2, USER_A)).isNotNull();
  }

  @Test
  void タスク配下削除成功_指定したタスクの作業セッションをすべて削除できること() {
    // Arrange
    assertThat(sut.findAllByTaskId(4, USER_A)).hasSize(2);
    assertThat(sut.findAllByTaskId(1, USER_A)).hasSize(1);

    // Act
    int actual = sut.deleteAllByTaskId(4);

    // Assert
    assertThat(actual).isEqualTo(2);
    assertThat(sut.findAllByTaskId(4, USER_A)).isEmpty();
    assertThat(sut.findAllByTaskId(1, USER_A)).hasSize(1);
  }

  @Test
  void タスク配下削除失敗_存在しないタスクIDの場合は削除されず0件となること() {
    // Act
    int actual = sut.deleteAllByTaskId(999);

    // Assert
    assertThat(actual).isZero();
  }

  private WorkSession insertTimerSession(int taskId, int seconds) {
    WorkSession workSession = new WorkSession();
    workSession.setTaskId(taskId);
    workSession.setType(WorkSessionType.TIMER);
    sut.insert(workSession);

    LocalDateTime startedAt = LocalDateTime.of(2026, 1, 3, 9, 0);
    workSession.setStartedAt(startedAt);
    workSession.setEndedAt(startedAt.plusSeconds(seconds));
    sut.update(workSession, USER_A);
    return sut.findById(workSession.getId(), USER_A);
  }

}
