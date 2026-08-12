package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.item_order.ProjectItemOrderReplaceRequest;
import com.kiborisaway.tasktimetracker.data.dto.item_order.ProjectItemOrderResponse;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorResponse;
import com.kiborisaway.tasktimetracker.service.ProjectItemOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class ProjectItemOrderController {

  private ProjectItemOrderService service;

  @Autowired
  public ProjectItemOrderController(ProjectItemOrderService service) {
    this.service = service;
  }

  @Operation(
      summary = "プロジェクト直下の並び順取得",
      description = "指定したプロジェクト直下のタスク・タスクグループの並び順をposition昇順で取得します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "pId", required = true,
              description = "プロジェクトID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "取得成功",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = ProjectItemOrderResponse.class))
              )
          ),
          @ApiResponse(
              responseCode = "404", description = "指定されたプロジェクトIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "400", description = "プロジェクトIDの形式が不正であったときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @GetMapping("/projects/{pId}/item-order")
  public List<ProjectItemOrderResponse> getAll(@PathVariable @Positive int pId) {
    return service.findAllInProject(pId);
  }

  @Operation(
      summary = "プロジェクト直下の並び替え",
      description = """
          プロジェクト直下のタスク・タスクグループの並び順を、リクエストで渡した配列の順序へ全置換します。
          リクエストの項目は、プロジェクト直下の現在の項目を過不足なく含む必要があります。
          """,
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "pId", required = true,
              description = "プロジェクトID",
              schema = @Schema(type = "integer", format = "int32")
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "並び替え後の順序どおりに並べた項目一覧",
          required = true,
          content = @Content(schema = @Schema(implementation = ProjectItemOrderReplaceRequest.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "並び替え成功",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = ProjectItemOrderResponse.class)))
          ),
          @ApiResponse(responseCode = "400",
              description = "入力値のバリデーションエラー、または項目の過不足・重複",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(responseCode = "404", description = "指定されたプロジェクトIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @PutMapping("/projects/{pId}/item-order")
  public List<ProjectItemOrderResponse> replace(
      @PathVariable @Positive int pId,
      @RequestBody @Validated ProjectItemOrderReplaceRequest request) {
    return service.replaceOrder(pId, request.getItems());
  }
}
