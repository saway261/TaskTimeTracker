package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.item_order.TaskGroupItemOrderReplaceRequest;
import com.kiborisaway.tasktimetracker.data.dto.item_order.TaskGroupItemOrderResponse;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorResponse;
import com.kiborisaway.tasktimetracker.publicid.id.TaskGroupId;
import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import com.kiborisaway.tasktimetracker.service.TaskGroupItemOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class TaskGroupItemOrderController {

  private TaskGroupItemOrderService service;

  @Autowired
  public TaskGroupItemOrderController(TaskGroupItemOrderService service) {
    this.service = service;
  }

  @Operation(
      summary = "タスクグループ直下の並び順取得",
      description = "指定したタスクグループ直下のタスクの並び順をposition昇順で取得します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "tgId", required = true,
              description = "タスクグループID",
              schema = @Schema(type = "string")
          )
      },
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "取得成功",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = TaskGroupItemOrderResponse.class))
              )
          ),
          @ApiResponse(
              responseCode = "404", description = "指定されたタスクグループIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "400", description = "タスクグループIDの形式が不正であったときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @GetMapping("/task-groups/{tgId}/item-order")
  public List<TaskGroupItemOrderResponse> getAll(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable TaskGroupId tgId) {
    return service.findAllInTaskGroup(user.getUserId(), tgId.value());
  }

  @Operation(
      summary = "タスクグループ直下の並び替え",
      description = """
          タスクグループ直下のタスクの並び順を、リクエストで渡した配列の順序へ全置換します。
          リクエストの項目は、タスクグループ直下の現在の項目を過不足なく含む必要があります。
          """,
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "tgId", required = true,
              description = "タスクグループID",
              schema = @Schema(type = "string")
          )
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "並び替え後の順序どおりに並べたタスクID一覧",
          required = true,
          content = @Content(schema = @Schema(implementation = TaskGroupItemOrderReplaceRequest.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "並び替え成功",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = TaskGroupItemOrderResponse.class)))
          ),
          @ApiResponse(responseCode = "400",
              description = "入力値のバリデーションエラー、または項目の過不足・重複",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(responseCode = "404", description = "指定されたタスクグループIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @PutMapping("/task-groups/{tgId}/item-order")
  public List<TaskGroupItemOrderResponse> replace(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable TaskGroupId tgId,
      @RequestBody @Validated TaskGroupItemOrderReplaceRequest request) {
    return service.replaceOrder(user.getUserId(), tgId.value(), request.getItems());
  }
}
