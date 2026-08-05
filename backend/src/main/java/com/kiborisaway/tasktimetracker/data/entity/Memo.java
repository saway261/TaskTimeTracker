package com.kiborisaway.tasktimetracker.data.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "メモ　タスクの実行を効率化するためにユーザが自由に活用可能")
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class Memo {

  @Schema(
      description = """
          メモID
          登録時は自動採番を行うため不要、更新時にはパスパラメータで指定するため不要なので、リクエストボディとしては常に不要
          """,
      example = "1")
  private Integer id;

  @Schema(description = "親プロジェクトID", example = "1")
  private Integer projectId;

  @Schema(description = "親タスクグループID", example = "1")
  private Integer taskGroupId;

  @Schema(description = "親タスクID", example = "1")
  private Integer taskId;

  @Schema(description = "プロジェクト名", example = "タスク管理アプリ開発")
  private String comment;


}

