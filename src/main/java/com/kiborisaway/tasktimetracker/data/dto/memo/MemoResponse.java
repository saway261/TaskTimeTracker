package com.kiborisaway.tasktimetracker.data.dto.memo;

import com.kiborisaway.tasktimetracker.data.entity.Memo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "メモレスポンス")
@Getter
public class MemoResponse {

  @Schema(description = "メモID", example = "1")
  private final int id;

  @Schema(description = "メモコメント", example = "明日は早起きする")
  private final String comment;

  public MemoResponse(Memo memo) {
    this.id = memo.getId();
    this.comment = memo.getComment();
  }
}
