package com.kiborisaway.tasktimetracker.data.dto.tag;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Schema(description = "タグ")
@Getter
@EqualsAndHashCode
public class TagResponse {

  @Schema(description = "タグID", example = "1")
  private final Integer id;

  @Schema(description = "タグ名", example = "調査")
  private final String name;

  @Schema(description = "アーカイブ済みフラグ アーカイブ済みは新規付与の候補と分析の絞り込みに出ない",
      example = "false")
  private final Boolean isArchived;

  @Schema(description = "付与タスク数 未完了を含む全タスクへの付与数。分析対象件数とは異なる", example = "12")
  private final Integer assignedTaskCount;

  public TagResponse(int id, String name, boolean isArchived, int assignedTaskCount) {
    this.id = id;
    this.name = name;
    this.isArchived = isArchived;
    this.assignedTaskCount = assignedTaskCount;
  }
}
