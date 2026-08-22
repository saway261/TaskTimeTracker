package com.kiborisaway.tasktimetracker.data.dto.task;

import com.kiborisaway.tasktimetracker.publicid.id.TagId;
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
      example = "[\"Xr9mQ2vKp3\", \"bN4tLp7WqZ\"]")
  @NotNull
  private List<TagId> tagIds;

}
