package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.kiborisaway.tasktimetracker.data.entity.Task;
import com.kiborisaway.tasktimetracker.data.entity.WorkSession;
import com.kiborisaway.tasktimetracker.data.entity.WorkSessionType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@MybatisTest
class TaskRepositoryTest {

  private static final int USER_A = 1;
  private static final int USER_B = 2;

  @Autowired
  private TaskRepository sut;

  @Autowired
  private WorkSessionRepository workSessionRepository;

  private static Task newTask(Integer projectId, Integer taskGroupId, String title,
      String description, Integer estimatedMinutes) {
    Task task = new Task();
    task.setProjectId(projectId);
    task.setTaskGroupId(taskGroupId);
    task.setTitle(title);
    task.setDescription(description);
    task.setEstimatedMinutes(estimatedMinutes);
    return task;
  }

  private void insertTimerSession(int taskId, int seconds) {
    WorkSession workSession = new WorkSession();
    workSession.setTaskId(taskId);
    workSession.setType(WorkSessionType.TIMER);
    workSessionRepository.insert(workSession);

    LocalDateTime startedAt = LocalDateTime.of(2026, 1, 3, 9, 0);
    workSession.setStartedAt(startedAt);
    workSession.setEndedAt(startedAt.plusSeconds(seconds));
    workSessionRepository.update(workSession, USER_A);
  }

  @Test
  void 全件検索_指定のタスクグループ配下のタスクの初期データを取得できること() {
    // Act
    List<Task> actual = sut.findAllInTaskGroup(1, USER_A);

    // Assert
    assertThat(actual)
        .extracting(Task::getTaskGroupId, Task::getTitle, Task::getDescription,
            Task::getEstimatedMinutes)
        .containsExactlyInAnyOrder(
            tuple(1, "カスタム例外作成", "カスタム例外クラスの作成とハンドリングを行う", 60),
            tuple(1, "バリデーション実装",
                "バリデーションの実装とバリデーション違反のハンドリングを行う", 120)
        );
  }

  @Test
  void 全件検索_存在しないタスクグループIDを指定すると空のリストを返すこと() {
    // Act
    List<Task> actual = sut.findAllInTaskGroup(999, USER_A);

    // Assert
    assertThat(actual).isEmpty();
  }

  @Test
  void 完了フラグ指定検索_指定のタスクグループ配下のタスクのうち完了済みのタスクのみを取得できること() {
    // Act
    List<Task> actual = sut.findAllInTaskGroupByCondition(2, true, USER_A);

    // Assert
    assertThat(actual)
        .extracting(Task::getTaskGroupId, Task::getTitle, Task::getDescription,
            Task::getEstimatedMinutes)
        .containsExactlyInAnyOrder(
            tuple(2, "コンポーネント作成", "共通UIコンポーネントを作成する", 90)
        );
    assertThat(actual).allSatisfy(task -> assertThat(task.getFinishedAt()).isNotNull());
  }

  @Test
  void 完了フラグ指定検索_指定のタスクグループ配下のタスクのうち未完了のタスクのみを取得できること() {
    // Act
    List<Task> actual = sut.findAllInTaskGroupByCondition(1, false, USER_A);

    // Assert
    assertThat(actual)
        .extracting(Task::getTaskGroupId, Task::getTitle, Task::getDescription,
            Task::getEstimatedMinutes)
        .containsExactlyInAnyOrder(
            tuple(1, "カスタム例外作成", "カスタム例外クラスの作成とハンドリングを行う", 60),
            tuple(1, "バリデーション実装",
                "バリデーションの実装とバリデーション違反のハンドリングを行う", 120)
        );
    assertThat(actual).allSatisfy(task -> assertThat(task.getFinishedAt()).isNull());
  }

  @Test
  void 完了フラグ指定検索_存在しないタスクグループIDを指定すると空のリストを返すこと() {
    // Act
    List<Task> actual = sut.findAllInTaskGroupByCondition(999, true, USER_A);

    // Assert
    assertThat(actual).isEmpty();
  }

  @Test
  void 全件検索_指定のプロジェクト配下のタスクの初期データを取得できること() {
    // Act
    List<Task> actual = sut.findAllInProject(1, USER_A);

    // Assert
    assertThat(actual)
        .extracting(Task::getProjectId, Task::getTaskGroupId, Task::getTitle)
        .containsExactlyInAnyOrder(
            tuple(null, 1, "カスタム例外作成"),
            tuple(null, 1, "バリデーション実装"),
            tuple(null, 2, "コンポーネント作成"),
            tuple(1, null, "画面設計")
        );
  }

  @Test
  void 全件検索_存在しないプロジェクトIDを指定すると空のリストを返すこと() {
    // Act
    List<Task> actual = sut.findAllInProject(999, USER_A);

    // Assert
    assertThat(actual).isEmpty();
  }

  @Test
  void 完了フラグ指定検索_指定のプロジェクト配下のタスクのうち完了済みのタスクのみを取得できること() {
    // Act
    List<Task> actual = sut.findAllInProjectByCondition(1, true, USER_A);

    // Assert
    assertThat(actual)
        .extracting(Task::getProjectId, Task::getTaskGroupId, Task::getTitle)
        .containsExactlyInAnyOrder(
            tuple(null, 2, "コンポーネント作成")
        );
    assertThat(actual).allSatisfy(task -> assertThat(task.getFinishedAt()).isNotNull());
  }

  @Test
  void 完了フラグ指定検索_指定のプロジェクト配下のタスクのうち未完了のタスクのみを取得できること() {
    // Act
    List<Task> actual = sut.findAllInProjectByCondition(1, false, USER_A);

    // Assert
    assertThat(actual)
        .extracting(Task::getProjectId, Task::getTaskGroupId, Task::getTitle)
        .containsExactlyInAnyOrder(
            tuple(null, 1, "カスタム例外作成"),
            tuple(null, 1, "バリデーション実装"),
            tuple(1, null, "画面設計")
        );
    assertThat(actual).allSatisfy(task -> assertThat(task.getFinishedAt()).isNull());
  }

  @Test
  void 完了フラグ指定検索_存在しないプロジェクトIDを指定すると空のリストを返すこと() {
    // Act
    List<Task> actual = sut.findAllInProjectByCondition(999, true, USER_A);

    // Assert
    assertThat(actual).isEmpty();
  }

  @Test
  void 未完了タスク存在チェック_プロジェクト内に未完了タスクがあればtrueを返すこと() {
    // Act
    boolean actual = sut.existsUnfinishedInProject(1, USER_A);

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  void 未完了タスク存在チェック_プロジェクト内の全タスクが完了済みならfalseを返すこと() {
    // Arrange（プロジェクト1配下の未完了タスク1,2,4を完了させる。3は初期データで完了済み）
    sut.updateFinished(1, true, USER_A);
    sut.updateFinished(2, true, USER_A);
    sut.updateFinished(4, true, USER_A);

    // Act
    boolean actual = sut.existsUnfinishedInProject(1, USER_A);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  void 未完了タスク存在チェック_プロジェクトの所有者が一致しない場合はfalseを返すこと() {
    // Act
    boolean actual = sut.existsUnfinishedInProject(1, USER_B);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  void 未完了タスク存在チェック_タスクグループ内に未完了タスクがあればtrueを返すこと() {
    // Act
    boolean actual = sut.existsUnfinishedInTaskGroup(1, USER_A);

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  void 未完了タスク存在チェック_タスクグループ内の全タスクが完了済みならfalseを返すこと() {
    // Act（タスクグループ2配下のタスク3は初期データで完了済み）
    boolean actual = sut.existsUnfinishedInTaskGroup(2, USER_A);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  void 未完了タスク存在チェック_タスクグループの所有者が一致しない場合はfalseを返すこと() {
    // Act
    boolean actual = sut.existsUnfinishedInTaskGroup(1, USER_B);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  void ID検索成功_IDが一致するタスクを取得できること() {
    // Arrange
    int id = 1;

    // Act
    Task actual = sut.findById(id, USER_A);

    // Assert
    assertThat(actual.getId()).isEqualTo(id);
    assertThat(actual.getTaskGroupId()).isEqualTo(1);
    assertThat(actual.getTitle()).isEqualTo("カスタム例外作成");
    assertThat(actual.getDescription()).isEqualTo("カスタム例外クラスの作成とハンドリングを行う");
    assertThat(actual.getEstimatedMinutes()).isEqualTo(60);
  }

  @Test
  void ID検索失敗_存在しないIDを指定するとnullを返すこと() {
    // Act
    Task actual = sut.findById(999, USER_A);

    // Assert
    assertThat(actual).isNull();
  }

  @Test
  void ID検索失敗_所有者が一致しない場合はnullを返すこと() {
    // Arrange
    int id = 1;

    // Act
    Task actual = sut.findById(id, USER_B);

    // Assert
    assertThat(actual).isNull();
  }

  @Test
  void ID存在チェック_存在するIDを指定するとtrueを返すこと() {
    // Arrange
    int id = 1;

    // Act
    boolean actual = sut.existsByIdAndUserId(id, USER_A);

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  void ID存在チェック_存在しないIDを指定するとfalseを返すこと() {
    // Arrange
    int id = 999;

    // Act
    boolean actual = sut.existsByIdAndUserId(id, USER_A);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  void ID存在チェック_所有者が一致しない場合はfalseを返すこと() {
    // Arrange
    int id = 1;

    // Act
    boolean actual = sut.existsByIdAndUserId(id, USER_B);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  void 完了状態チェック_完了済みタスクIDを指定するとtrueを返すこと() {
    // Arrange
    int id = 3;

    // Act
    boolean actual = sut.isFinished(id, USER_A);

    // Assert
    assertThat(actual).isTrue();
  }

  @Test
  void 完了状態チェック_未完了タスクIDを指定するとfalseを返すこと() {
    // Arrange
    int id = 1;

    // Act
    boolean actual = sut.isFinished(id, USER_A);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  void 完了状態チェック_存在しないタスクIDを指定するとfalseを返すこと() {
    // Arrange
    int id = 999;

    // Act
    boolean actual = sut.isFinished(id, USER_A);

    // Assert
    assertThat(actual).isFalse();
  }

  @Test
  void 登録成功_タスクグループIDのみを指定したタスクを登録でき採番されたidが設定されること() {
    // Arrange
    List<Task> before = sut.findAllInProject(1, USER_A);
    Task task = newTask(null, 1, "API実装", "タスクAPIを実装する", 100);

    // Act
    sut.insert(task);

    List<Task> after = sut.findAllInProject(1, USER_A);
    Task registered = sut.findById(task.getId(), USER_A);

    // Assert
    assertThat(task.getId()).isNotNull();
    assertThat(after).hasSize(before.size() + 1);
    assertThat(registered.getProjectId()).isNull();
    assertThat(registered.getTaskGroupId()).isEqualTo(1);
    assertThat(registered.getTitle()).isEqualTo("API実装");
    assertThat(registered.getDescription()).isEqualTo("タスクAPIを実装する");
    assertThat(registered.getEstimatedMinutes()).isEqualTo(100);
    assertThat(registered.getCreatedAt()).isNotNull();
    assertThat(registered.getFinishedAt()).isNull();
  }

  @Test
  void 登録成功_プロジェクトIDのみを指定したタスクを登録できること() {
    // Arrange
    List<Task> before = sut.findAllInProject(1, USER_A);
    Task task = newTask(1, null, "要件整理", "プロジェクト直下のタスクを登録する", 80);

    // Act
    sut.insert(task);

    List<Task> after = sut.findAllInProject(1, USER_A);
    Task registered = sut.findById(task.getId(), USER_A);

    // Assert
    assertThat(task.getId()).isNotNull();
    assertThat(after).hasSize(before.size() + 1);
    assertThat(registered.getProjectId()).isEqualTo(1);
    assertThat(registered.getTaskGroupId()).isNull();
    assertThat(registered.getTitle()).isEqualTo("要件整理");
    assertThat(registered.getDescription()).isEqualTo("プロジェクト直下のタスクを登録する");
    assertThat(registered.getEstimatedMinutes()).isEqualTo(80);
  }

  @Test
  void 登録失敗_titleがnullの場合は例外が発生すること() {
    // Arrange
    Task task = newTask(null, 1, null, "説明", 60);

    // Assert
    assertThatThrownBy(() -> sut.insert(task))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 登録失敗_プロジェクトIDとタスクグループIDがどちらもnullの場合は例外が発生すること() {
    // Arrange
    Task task = newTask(null, null, "親なしタスク", "親を指定しない", 60);

    // Assert
    assertThatThrownBy(() -> sut.insert(task))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 登録失敗_プロジェクトIDとタスクグループIDがどちらも指定された場合は例外が発生すること() {
    // Arrange
    Task task = newTask(1, 1, "親重複タスク", "親を重複指定する", 60);

    // Assert
    assertThatThrownBy(() -> sut.insert(task))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 更新成功_既存タスクのタイトルと説明だけを更新できること() {
    // Arrange
    int id = 1;
    Task before = sut.findById(id, USER_A);
    Task task = newTask(2, null, "例外設計", "例外設計を見直す", 999);
    task.setId(id);

    // Act
    int actual = sut.updateProperty(task, USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);

    Task updated = sut.findById(id, USER_A);
    assertThat(updated.getTitle()).isEqualTo("例外設計");
    assertThat(updated.getDescription()).isEqualTo("例外設計を見直す");
    assertThat(updated.getProjectId()).isEqualTo(before.getProjectId());
    assertThat(updated.getTaskGroupId()).isEqualTo(before.getTaskGroupId());
    assertThat(updated.getEstimatedMinutes()).isEqualTo(before.getEstimatedMinutes());
    assertThat(updated.getCreatedAt()).isEqualTo(before.getCreatedAt());
    assertThat(updated.getFinishedAt()).isEqualTo(before.getFinishedAt());
    assertThat(updated.getActualMinutesCached()).isEqualTo(before.getActualMinutesCached());
    assertThat(updated.getGapMinutesCached()).isEqualTo(before.getGapMinutesCached());
    assertThat(updated.getGapRateCached()).isEqualTo(before.getGapRateCached());
  }

  @Test
  void 更新失敗_タイトルと説明の更新で存在しないIDの場合は更新されず0件となること() {
    // Arrange
    List<Task> before = sut.findAllInProject(1, USER_A);
    Task task = newTask(null, 1, "更新されないタイトル", "更新されない説明", 60);
    task.setId(999);

    // Act
    int actual = sut.updateProperty(task, USER_A);

    // Assert
    assertThat(actual).isEqualTo(0);
    assertThat(sut.findAllInProject(1, USER_A))
        .usingRecursiveComparison()
        .isEqualTo(before);
  }

  @Test
  void 更新失敗_titleがnullの場合は例外が発生すること() {
    // Arrange
    Task task = newTask(null, 1, null, "説明", 60);
    task.setId(1);

    // Assert
    assertThatThrownBy(() -> sut.updateProperty(task, USER_A))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 所属変更成功_プロジェクト直下のタスクをタスクグループ配下に変更できること() {
    // Arrange
    int id = 4;

    // Act
    int actual = sut.updateTaskGroup(id, 2, USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);

    Task updated = sut.findById(id, USER_A);
    assertThat(updated.getProjectId()).isNull();
    assertThat(updated.getTaskGroupId()).isEqualTo(2);
  }

  @Test
  void 所属変更成功_タスクグループ配下のタスクを別のタスクグループ配下に変更できること() {
    // Arrange
    int id = 1;

    // Act
    int actual = sut.updateTaskGroup(id, 2, USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);

    Task updated = sut.findById(id, USER_A);
    assertThat(updated.getProjectId()).isNull();
    assertThat(updated.getTaskGroupId()).isEqualTo(2);
  }

  @Test
  void 所属変更失敗_存在しないタスクIDの場合は更新されず0件となること() {
    // Arrange
    List<Task> before = sut.findAllInProject(1, USER_A);

    // Act
    int actual = sut.updateTaskGroup(999, 2, USER_A);

    // Assert
    assertThat(actual).isEqualTo(0);
    assertThat(sut.findAllInProject(1, USER_A))
        .usingRecursiveComparison()
        .isEqualTo(before);
  }

  @Test
  void 所属変更成功_タスクグループ配下のタスクをプロジェクト直下に変更できること() {
    // Arrange
    int id = 1;

    // Act
    int actual = sut.updateProject(id, 1, USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);

    Task updated = sut.findById(id, USER_A);
    assertThat(updated.getProjectId()).isEqualTo(1);
    assertThat(updated.getTaskGroupId()).isNull();
  }

  @Test
  void 所属変更失敗_プロジェクト直下への変更で存在しないタスクIDの場合は更新されず0件となること() {
    // Arrange
    List<Task> before = sut.findAllInProject(1, USER_A);

    // Act
    int actual = sut.updateProject(999, 1, USER_A);

    // Assert
    assertThat(actual).isEqualTo(0);
    assertThat(sut.findAllInProject(1, USER_A))
        .usingRecursiveComparison()
        .isEqualTo(before);
  }

  @Test
  void 更新成功_既存タスクの見積もり作業時間だけを更新できること() {
    // Arrange
    int id = 1;
    Task before = sut.findById(id, USER_A);
    Task task = newTask(2, null, "更新されないタイトル", "更新されない説明", 45);
    task.setId(id);

    // Act
    int actual = sut.updateEstimateMinutes(task.getId(), task.getEstimatedMinutes(), USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);

    Task updated = sut.findById(id, USER_A);
    assertThat(updated.getEstimatedMinutes()).isEqualTo(45);
    assertThat(updated.getProjectId()).isEqualTo(before.getProjectId());
    assertThat(updated.getTaskGroupId()).isEqualTo(before.getTaskGroupId());
    assertThat(updated.getTitle()).isEqualTo(before.getTitle());
    assertThat(updated.getDescription()).isEqualTo(before.getDescription());
    assertThat(updated.getCreatedAt()).isEqualTo(before.getCreatedAt());
    assertThat(updated.getFinishedAt()).isEqualTo(before.getFinishedAt());
    assertThat(updated.getActualMinutesCached()).isEqualTo(before.getActualMinutesCached());
    assertThat(updated.getGapMinutesCached()).isEqualTo(before.getGapMinutesCached());
    assertThat(updated.getGapRateCached()).isEqualTo(before.getGapRateCached());
  }

  @Test
  void 更新失敗_見積もり作業時間の更新で存在しないIDの場合は更新されず0件となること() {
    // Arrange
    List<Task> before = sut.findAllInProject(1, USER_A);
    Task task = newTask(null, 1, "更新されないタイトル", "更新されない説明", 45);
    task.setId(999);

    // Act
    int actual = sut.updateEstimateMinutes(task.getId(), task.getEstimatedMinutes(), USER_A);

    // Assert
    assertThat(actual).isEqualTo(0);
    assertThat(sut.findAllInProject(1, USER_A))
        .usingRecursiveComparison()
        .isEqualTo(before);
  }

  @Test
  void 完了更新成功_既存タスクを完了にして実績時間と差分をキャッシュできること() {
    // Act
    int actual = sut.updateFinished(4, true, USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);

    Task updated = sut.findById(4, USER_A);
    assertThat(updated.getFinishedAt()).isNotNull();
    assertThat(updated.getActualMinutesCached()).isEqualTo(75);
    assertThat(updated.getGapMinutesCached()).isEqualTo(-105);
    assertThat(updated.getGapRateCached()).isEqualTo(-58.333333333333336);
  }

  @Test
  void 完了更新成功_WorkSessionの秒数を合計してから分へ切り捨ててキャッシュすること() {
    // Arrange
    insertTimerSession(2, 30);
    insertTimerSession(2, 30);

    // Act
    int actual = sut.updateFinished(2, true, USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);
    Task updated = sut.findById(2, USER_A);
    assertThat(updated.getActualMinutesCached()).isEqualTo(1);
    assertThat(updated.getGapMinutesCached()).isEqualTo(-119);
    assertThat(updated.getGapRateCached()).isEqualTo(-99.16666666666667);
  }

  @Test
  void 完了更新成功_完了済みタスクを未完了に戻して完了日時とキャッシュをnullにできること() {
    // Act
    int actual = sut.updateFinished(3, false, USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);

    Task updated = sut.findById(3, USER_A);
    assertThat(updated.getFinishedAt()).isNull();
    assertThat(updated.getActualMinutesCached()).isNull();
    assertThat(updated.getGapMinutesCached()).isNull();
    assertThat(updated.getGapRateCached()).isNull();
  }

  @Test
  void 完了更新失敗_存在しないIDの場合は更新されず0件となること() {
    // Arrange
    List<Task> before = sut.findAllInProject(1, USER_A);

    // Act
    int actual = sut.updateFinished(999, true, USER_A);

    // Assert
    assertThat(actual).isEqualTo(0);
    assertThat(sut.findAllInProject(1, USER_A))
        .usingRecursiveComparison()
        .isEqualTo(before);
  }

  @Test
  void 削除成功_既存タスクをID指定で削除できること() {
    // Arrange
    int id = 2;
    List<Task> before = sut.findAllInProject(1, USER_A);

    // Act
    int actual = sut.deleteById(id, USER_A);

    // Assert
    assertThat(actual).isEqualTo(1);
    assertThat(sut.findById(id, USER_A)).isNull();
    assertThat(sut.findAllInProject(1, USER_A)).hasSize(before.size() - 1);
  }

  @Test
  void 削除失敗_存在しないIDの場合は削除されず0件となること() {
    // Arrange
    List<Task> before = sut.findAllInProject(1, USER_A);

    // Act
    int actual = sut.deleteById(999, USER_A);

    // Assert
    assertThat(actual).isEqualTo(0);
    assertThat(sut.findAllInProject(1, USER_A))
        .usingRecursiveComparison()
        .isEqualTo(before);
  }

}
