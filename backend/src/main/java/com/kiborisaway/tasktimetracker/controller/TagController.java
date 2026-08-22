package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.dto.tag.TagCreateRequest;
import com.kiborisaway.tasktimetracker.data.dto.tag.TagResponse;
import com.kiborisaway.tasktimetracker.data.dto.tag.TagUpdateArchivedRequest;
import com.kiborisaway.tasktimetracker.data.dto.tag.TagUpdateRequest;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorResponse;
import com.kiborisaway.tasktimetracker.publicid.id.TagId;
import com.kiborisaway.tasktimetracker.security.AuthenticatedUser;
import com.kiborisaway.tasktimetracker.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/tags")
public class TagController {

  private TagService service;

  @Autowired
  public TagController(TagService service) {
    this.service = service;
  }

  @Operation(
      summary = "タグ全件取得（アーカイブ済み含む指定可）",
      description = """
          認証ユーザーが保有するタグを、付与タスク数つきで一覧取得します。
          付与タスク数の降順、同数の場合は名前の昇順で返します。
          includeArchivedを省略、またはfalseにするとアーカイブ済みタグは含まれません。
          """,
      responses = {
          @ApiResponse(responseCode = "200", description = "取得成功",
              content = @Content(mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = TagResponse.class))))
      }
  )
  @GetMapping
  public List<TagResponse> getAll(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @RequestParam(defaultValue = "false") boolean includeArchived) {
    return service.findAll(user.getUserId(), includeArchived);
  }

  @Operation(
      summary = "タグ新規登録",
      description = """
          タグの新規登録を行います。
          正規化後（大文字小文字・全角半角の違いを吸収した後）の名前が既存タグと一致する場合、
          新規作成は行わず既存タグを200で返します。新規作成した場合は201で返します。
          """,
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "新規に登録したいタグの詳細",
          required = true,
          content = @Content(schema = @Schema(implementation = TagCreateRequest.class))
      ),
      responses = {
          @ApiResponse(responseCode = "201", description = "新規作成成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = TagResponse.class))),
          @ApiResponse(responseCode = "200", description = "既存タグを再利用",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = TagResponse.class))),
          @ApiResponse(responseCode = "400",
              description = "入力値のバリデーションエラー、または保有タグ数が上限に達している場合",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)))
      }
  )
  @PostMapping
  public ResponseEntity<TagResponse> create(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @RequestBody @Validated TagCreateRequest request) {
    TagService.CreateResult result = service.create(user.getUserId(), request);
    HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
    return ResponseEntity.status(status).body(result.tag());
  }

  @Operation(
      summary = "タグ名変更",
      description = "タグ名を変更します。正規化後の名前が自分自身以外の既存タグと重複する場合は更新できません。",
      parameters = {
          @Parameter(in = ParameterIn.PATH, name = "tagId", required = true,
              description = "タグID",
              schema = @Schema(type = "string"))
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "変更後のタグ名",
          required = true,
          content = @Content(schema = @Schema(implementation = TagUpdateRequest.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "更新成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = TagResponse.class))),
          @ApiResponse(responseCode = "400",
              description = "入力値のバリデーションエラー、または名前が重複している場合",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))),
          @ApiResponse(responseCode = "404", description = "指定されたタグIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)))
      }
  )
  @PutMapping("/{tagId}")
  public ResponseEntity<TagResponse> update(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable TagId tagId,
      @RequestBody @Validated TagUpdateRequest request) {
    return ResponseEntity.ok(service.update(user.getUserId(), tagId.value(), request));
  }

  @Operation(
      summary = "タグのアーカイブ状態更新",
      description = """
          タグのアーカイブ状態を更新します。
          アーカイブ済みタグを解除する場合、保有タグ数（アクティブなタグ）が上限に達していると更新できません。
          状態が変わらない要求（アクティブなタグへのisArchived=falseなど）は上限に関係なく成功します。
          """,
      parameters = {
          @Parameter(in = ParameterIn.PATH, name = "tagId", required = true,
              description = "タグID",
              schema = @Schema(type = "string"))
      },
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "アーカイブ状態フラグ（true: アーカイブする、false: 解除する）",
          required = true,
          content = @Content(schema = @Schema(implementation = TagUpdateArchivedRequest.class))
      ),
      responses = {
          @ApiResponse(responseCode = "200", description = "更新成功",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = TagResponse.class))),
          @ApiResponse(responseCode = "400",
              description = "入力値のバリデーションエラー、または保有タグ数が上限に達している場合",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))),
          @ApiResponse(responseCode = "404", description = "指定されたタグIDが存在しないときのエラー",
              content = @Content(mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)))
      }
  )
  @PatchMapping("/{tagId}/archived")
  public ResponseEntity<TagResponse> updateArchived(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable TagId tagId,
      @RequestBody @Validated TagUpdateArchivedRequest request) {
    return ResponseEntity.ok(
        service.updateArchived(user.getUserId(), tagId.value(), request.isArchived()));
  }
}
