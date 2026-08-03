package com.kiborisaway.tasktimetracker.data.entity;

import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.task_group.TaskGroupUpdateRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "タスクグループ")
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class TaskGroup {

  @Schema(
      description = """
          タスクグループID
          登録時は自動採番を行うため不要、更新時にはパスパラメータで指定するため不要なので、リクエストボディとしては常に不要
          """,
      example = "1")
  private Integer id;

  @Schema(
      description = """
          親となるプロジェクトID
          登録時はパスパラメータで指定し、更新不可なので、リクエストボディとしては常に不要
          """,
      example = "1")
  private Integer projectId;

  @Schema(description = "タスクグループ名", example = "環境構築")
  private String title;

  @Schema(description = "タスクグループの説明　省略可", example = "Dockerを使う")
  private String description;

  @Schema(description = "完了フラグ", example = "false")
  private Boolean isFinished;

  // 新規登録用
  public TaskGroup(TaskGroupCreateRequest request) {
    this.title = request.getTitle();
    this.description = request.getDescription();
  }

  // 更新用
  public TaskGroup(int id, TaskGroupUpdateRequest request) {
    this.id = id;
    this.title = request.getTitle();
    this.description = request.getDescription();
    this.isFinished = request.getIsFinished();
  }

}
