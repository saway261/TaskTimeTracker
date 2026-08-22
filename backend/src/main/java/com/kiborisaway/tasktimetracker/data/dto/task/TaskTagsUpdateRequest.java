package com.kiborisaway.tasktimetracker.data.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "タスクのタグ全置換リクエスト")
@Getter
@Setter
public class TaskTagsUpdateRequest {

  @Schema(description = "タスクに付与するタグIDの配列 全置換。空配列を指定するとタグなしになる。件数の上限はない",
      example = "[1, 2]")
  @NotNull
  private List<Integer> tagIds;

}
