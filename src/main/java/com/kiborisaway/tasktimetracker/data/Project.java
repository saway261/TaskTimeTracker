package com.kiborisaway.tasktimetracker.data;

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

@Schema(description = "プロジェクト")
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class Project {

  @Schema(
      description = """
          プロジェクトID
          登録時は自動採番を行うため不要、更新時にはパスパラメータで指定するため不要なので、リクエストボディとしては常に不要
          """,
      example = "1")
  private Integer id;

  @Schema(description = "プロジェクト名", example = "タスク管理アプリ開発")
  @NotBlank(groups = {CreateGroup.class, UpdateGroup.class})
  @Size(max = 20, groups = {CreateGroup.class, UpdateGroup.class})
  private String title;

  @Schema(description = "プロジェクトの説明　省略可", example = "自主開発")
  @Size(max = 200, groups = {CreateGroup.class, UpdateGroup.class})
  private String description;

  @Schema(description = "完了フラグ", example = "false")
  @NotNull(groups = {UpdateGroup.class})
  private Boolean isFinished;


}


