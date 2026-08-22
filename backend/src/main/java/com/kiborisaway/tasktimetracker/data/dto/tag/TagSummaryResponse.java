package com.kiborisaway.tasktimetracker.data.dto.tag;

import com.kiborisaway.tasktimetracker.publicid.id.TagId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "タスク・振り返り・散布図に埋め込むタグの要約")
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class TagSummaryResponse {

  @Schema(description = "タグID", example = "Xr9mQ2vKp3")
  private TagId id;

  @Schema(description = "タグ名", example = "調査")
  private String name;
}
