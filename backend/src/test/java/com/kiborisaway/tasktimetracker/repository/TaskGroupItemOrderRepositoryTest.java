package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.kiborisaway.tasktimetracker.data.entity.TaskGroupItemOrder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@MybatisTest
class TaskGroupItemOrderRepositoryTest {

  @Autowired
  private TaskGroupItemOrderRepository sut;

  @Test
  void 全件検索_並び順レコードが存在しない場合は空リストを返すこと() {
    // Act
    List<TaskGroupItemOrder> actual = sut.findAllInTaskGroupOrdered(1);

    // Assert
    assertThat(actual).isEmpty();
  }

  @Test
  void 末尾追加成功_タスクを追加するたびpositionが連番で採番されること() {
    // Arrange
    int tgId = 1;

    // Act
    sut.insertAppendForTask(tgId, 1); // position=0
    sut.insertAppendForTask(tgId, 2); // position=1

    // Assert
    List<TaskGroupItemOrder> actual = sut.findAllInTaskGroupOrdered(tgId);
    assertThat(actual)
        .extracting(TaskGroupItemOrder::getTaskGroupId, TaskGroupItemOrder::getTaskId,
            TaskGroupItemOrder::getPosition)
        .containsExactly(
            tuple(tgId, 1, 0),
            tuple(tgId, 2, 1)
        );
  }

  @Test
  void 末尾追加失敗_同一タスクを二重登録すると一意制約違反になること() {
    // Arrange
    sut.insertAppendForTask(1, 1);

    // Act & Assert : task_idはUNIQUEのため二重登録は制約違反になる
    assertThatThrownBy(() -> sut.insertAppendForTask(1, 1))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void position更新成功_対象のタスクの並び順を更新できること() {
    // Arrange
    sut.insertAppendForTask(1, 1);

    // Act
    int actual = sut.updatePositionByTaskId(1, 5);

    // Assert
    assertThat(actual).isEqualTo(1);
    assertThat(sut.findAllInTaskGroupOrdered(1))
        .extracting(TaskGroupItemOrder::getTaskId, TaskGroupItemOrder::getPosition)
        .containsExactly(tuple(1, 5));
  }

  @Test
  void position更新失敗_存在しないタスクIDを指定すると更新件数0件となること() {
    // Act
    int actual = sut.updatePositionByTaskId(999, 0);

    // Assert
    assertThat(actual).isEqualTo(0);
  }

  @Test
  void position更新失敗_同一タスクグループ内で他の項目とpositionが重複すると一意制約違反になること() {
    // Arrange
    sut.insertAppendForTask(1, 1); // position=0
    sut.insertAppendForTask(1, 2); // position=1

    // Act & Assert
    assertThatThrownBy(() -> sut.updatePositionByTaskId(2, 0))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 削除成功_対象のタスクの並び順レコードを削除できること() {
    // Arrange
    sut.insertAppendForTask(1, 1);

    // Act
    int actual = sut.deleteByTaskId(1);

    // Assert
    assertThat(actual).isEqualTo(1);
    assertThat(sut.findAllInTaskGroupOrdered(1)).isEmpty();
  }

  @Test
  void 削除失敗_存在しないタスクIDを指定すると削除件数0件となること() {
    // Act
    int actual = sut.deleteByTaskId(999);

    // Assert
    assertThat(actual).isEqualTo(0);
  }

  @Test
  void 全件削除成功_タスクグループIDを指定して並び順レコードを全件削除できること() {
    // Arrange
    sut.insertAppendForTask(1, 1);
    sut.insertAppendForTask(1, 2);

    // Act
    int actual = sut.deleteByTaskGroupId(1);

    // Assert
    assertThat(actual).isEqualTo(2);
    assertThat(sut.findAllInTaskGroupOrdered(1)).isEmpty();
  }

}
