package com.kiborisaway.tasktimetracker.data.dto.memo;

import com.kiborisaway.tasktimetracker.data.entity.Memo;
import com.kiborisaway.tasktimetracker.publicid.id.MemoId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "メモレスポンス")
@Getter
public class MemoResponse {

  @Schema(description = "メモID", example = "Xr9mQ2vKp3")
  private final MemoId id;

  @Schema(description = "メモコメント", example = "明日は早起きする")
  private final String comment;

  public MemoResponse(Memo memo) {
    this.id = new MemoId(memo.getId());
    this.comment = memo.getComment();
  }
}
