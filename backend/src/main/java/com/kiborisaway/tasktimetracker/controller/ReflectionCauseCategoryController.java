package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.reflection.ReflectionCauseCategoryResponse;
import com.kiborisaway.tasktimetracker.service.ReflectionCauseCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReflectionCauseCategoryController {

  private ReflectionCauseCategoryService service;

  @Autowired
  public ReflectionCauseCategoryController(ReflectionCauseCategoryService service) {
    this.service = service;
  }

  @Operation(
      summary = "原因カテゴリ一覧取得",
      description = "振り返りの原因として選択できる、有効な原因カテゴリを表示順で返します。"
          + "1件の振り返りには1〜3件を選択できます。",
      responses = {
          @ApiResponse(responseCode = "200", description = "取得成功",
              content = @Content(mediaType = "application/json",
                  array = @ArraySchema(
                      schema = @Schema(implementation = ReflectionCauseCategoryResponse.class))))
      }
  )
  @GetMapping("/reflection-cause-categories")
  public List<ReflectionCauseCategoryResponse> getAll() {
    return service.findAllActive();
  }
}
