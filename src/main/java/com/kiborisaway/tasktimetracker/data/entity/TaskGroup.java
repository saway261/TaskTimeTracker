package com.kiborisaway.tasktimetracker.data.entity;

import com.kiborisaway.tasktimetracker.validation.CreateGroup;
import com.kiborisaway.tasktimetracker.validation.UpdateGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
  @NotBlank(groups = {CreateGroup.class, UpdateGroup.class})
  @Size(max = 20, groups = {CreateGroup.class, UpdateGroup.class})
  private String title;

  @Schema(description = "タスクグループの説明　省略可", example = "Dockerを使う")
  @Size(max = 200, groups = {CreateGroup.class, UpdateGroup.class})
  private String description;

  @Schema(description = "完了フラグ", example = "false")
  @NotNull(groups = {UpdateGroup.class})
  private Boolean isFinished;

}
