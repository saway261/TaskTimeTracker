package com.kiborisaway.tasktimetracker.exception.handler;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Schema(description = "エラーレスポンス")
@AllArgsConstructor
@Getter
public class ErrorResponse {

  @Schema(description = "HTTPステータスコード", example = "400 BAD_REQUEST")
  private final HttpStatus status;

  @Schema(description = "エラーの概要", example = "validation error")
  private final String message;

  @Schema(description = "エラーの詳細", example = """
        [
                {
                    "field": "project.id",
                    "message": "指定したIDのプロジェクトは見つかりませんでした"
                }
            ]
      """
  )
  private final List<Map<String, String>> errors;

}