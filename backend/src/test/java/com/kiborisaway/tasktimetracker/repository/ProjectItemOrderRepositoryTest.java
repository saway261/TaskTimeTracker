package com.kiborisaway.tasktimetracker.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.kiborisaway.tasktimetracker.data.entity.ProjectItemOrder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@MybatisTest
class ProjectItemOrderRepositoryTest {

  @Autowired
  private ProjectItemOrderRepository sut;

  @Test
  void 全件検索_並び順レコードが存在しない場合は空リストを返すこと() {
    // Act
    List<ProjectItemOrder> actual = sut.findAllInProjectOrdered(1);

    // Assert
    assertThat(actual).isEmpty();
  }

  @Test
  void 末尾追加成功_タスクとタスクグループを追加するたびpositionが連番で採番されること() {
    // Arrange
    int pId = 1;

    // Act
    sut.insertAppendForTask(pId, 4); // position=0
    sut.insertAppendForTaskGroup(pId, 1); // position=1
    sut.insertAppendForTaskGroup(pId, 2); // position=2

    // Assert
    List<ProjectItemOrder> actual = sut.findAllInProjectOrdered(pId);
    assertThat(actual)
        .extracting(ProjectItemOrder::getProjectId, ProjectItemOrder::getTaskId,
            ProjectItemOrder::getTaskGroupId, ProjectItemOrder::getPosition)
        .containsExactly(
            tuple(pId, 4, null, 0),
            tuple(pId, null, 1, 1),
            tuple(pId, null, 2, 2)
        );
  }

  @Test
  void 末尾追加成功_同一タスクをプロジェクトが異なる別スコープへ追加できないこと() {
    // Arrange
    sut.insertAppendForTask(1, 4);

    // Act & Assert : task_idはUNIQUEのため二重登録は制約違反になる
    assertThatThrownBy(() -> sut.insertAppendForTask(1, 4))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void position更新成功_対象のタスクの並び順を更新できること() {
    // Arrange
    sut.insertAppendForTask(1, 4);

    // Act
    int actual = sut.updatePositionByTaskId(4, 5);

    // Assert
    assertThat(actual).isEqualTo(1);
    assertThat(sut.findAllInProjectOrdered(1))
        .extracting(ProjectItemOrder::getTaskId, ProjectItemOrder::getPosition)
        .containsExactly(tuple(4, 5));
  }

  @Test
  void position更新成功_対象のタスクグループの並び順を更新できること() {
    // Arrange
    sut.insertAppendForTaskGroup(1, 1);

    // Act
    int actual = sut.updatePositionByTaskGroupId(1, 9);

    // Assert
    assertThat(actual).isEqualTo(1);
    assertThat(sut.findAllInProjectOrdered(1))
        .extracting(ProjectItemOrder::getTaskGroupId, ProjectItemOrder::getPosition)
        .containsExactly(tuple(1, 9));
  }

  @Test
  void position更新失敗_存在しないタスクIDを指定すると更新件数0件となること() {
    // Act
    int actual = sut.updatePositionByTaskId(999, 0);

    // Assert
    assertThat(actual).isEqualTo(0);
  }

  @Test
  void position更新失敗_同一プロジェクト内で他の項目とpositionが重複すると一意制約違反になること() {
    // Arrange
    sut.insertAppendForTask(1, 4); // position=0
    sut.insertAppendForTaskGroup(1, 1); // position=1

    // Act & Assert
    assertThatThrownBy(() -> sut.updatePositionByTaskGroupId(1, 0))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void 削除成功_対象のタスクの並び順レコードを削除できること() {
    // Arrange
    sut.insertAppendForTask(1, 4);

    // Act
    int actual = sut.deleteByTaskId(4);

    // Assert
    assertThat(actual).isEqualTo(1);
    assertThat(sut.findAllInProjectOrdered(1)).isEmpty();
  }

  @Test
  void 削除失敗_存在しないタスクIDを指定すると削除件数0件となること() {
    // Act
    int actual = sut.deleteByTaskId(999);

    // Assert
    assertThat(actual).isEqualTo(0);
  }

  @Test
  void 削除成功_対象のタスクグループの並び順レコードを削除できること() {
    // Arrange
    sut.insertAppendForTaskGroup(1, 1);

    // Act
    int actual = sut.deleteByTaskGroupId(1);

    // Assert
    assertThat(actual).isEqualTo(1);
    assertThat(sut.findAllInProjectOrdered(1)).isEmpty();
  }

  @Test
  void 削除失敗_存在しないタスクグループIDを指定すると削除件数0件となること() {
    // Act
    int actual = sut.deleteByTaskGroupId(999);

    // Assert
    assertThat(actual).isEqualTo(0);
  }

}
