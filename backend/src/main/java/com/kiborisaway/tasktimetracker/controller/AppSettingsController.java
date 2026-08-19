package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.settings.AppSettingsResponse;
import com.kiborisaway.tasktimetracker.service.AppSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppSettingsController {

  private AppSettingsService service;

  @Autowired
  public AppSettingsController(AppSettingsService service) {
    this.service = service;
  }

  @Operation(
      summary = "アプリ設定取得",
      description = "分析・振り返り・タスク管理の各画面が共通で参照するアプリ設定を返します。",
      responses = {
          @ApiResponse(responseCode = "200", description = "取得成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = AppSettingsResponse.class)))
      }
  )
  @GetMapping("/app-settings")
  public AppSettingsResponse get() {
    return service.getSettings();
  }
}
