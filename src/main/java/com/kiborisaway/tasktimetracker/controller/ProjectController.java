package com.kiborisaway.tasktimetracker.controller;

import com.kiborisaway.tasktimetracker.data.entity.Project;
import com.kiborisaway.tasktimetracker.exception.handler.ErrorResponse;
import com.kiborisaway.tasktimetracker.service.ProjectService;
import com.kiborisaway.tasktimetracker.validation.CreateGroup;
import com.kiborisaway.tasktimetracker.validation.UpdateGroup;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/projects")
public class ProjectController {

  private ProjectService service;

  @Autowired
  public ProjectController(ProjectService service) {
    this.service = service;
  }

  @Operation(
      summary = "プロジェクト全件取得（完了フラグ指定可）",
      description = """
          完了フラグを指定して、完了済みのプロジェクトおよび未完了のプロジェクトをリストで取得します。
          例: /api/projects?isFinished=false
          クエリパラメータを省略した場合は、プロジェクトを全件取得します。
          """,
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "検索成功",
              content = @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = Project.class))
              )
          ),
          @ApiResponse(
              responseCode = "400",
              description = "クエリパラメータの形式が不正であったときのエラー",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)
              )
          )
      }
  )
  @GetMapping
  public List<Project> getAll(@RequestParam(required = false) Boolean isFinished) {
    return service.findAllByCondition(isFinished);
  }

  @Operation(
      summary = "プロジェクトID検索",
      description = "プロジェクトの全件からIDが一致するプロジェクトを取得します。",
      parameters = {
          @Parameter(in = ParameterIn.PATH,
              name = "projectId", required = true,
              description = "プロジェクトID",
              schema = @Schema(
                  type = "integer",
                  format = "int32"
              )
          )},
      responses = {
          @ApiResponse(
              responseCode = "200", description = "ok",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Project.class)
              )),
          @ApiResponse(
              responseCode = "404", description = "指定されたプロジェクトIDが存在しなかったときのエラー",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)
              )),
          @ApiResponse(
              responseCode = "400", description = "プロジェクトIDの形式が不正であったときのエラー",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class)
              ))
      }
  )
  @GetMapping("/{id}")
  public Project getById(@PathVariable @Positive int id) {
    return service.findById(id);
  }

  @Operation(
      summary = "プロジェクト新規登録",
      description = "プロジェクトの登録を行います",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "新規に登録したいプロジェクトの詳細",
          required = true,
          content = @Content(
              schema = @Schema(implementation = Project.class)
          )
      ),
      responses = {
          @ApiResponse(
              responseCode = "201", description = "ok",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Project.class)
              )
          ),
          @ApiResponse(
              responseCode = "400", description = "入力値のバリデーションエラー",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Project.class))
          )
      }
  )
  @PostMapping
  public ResponseEntity<Project> create(
      @RequestBody @Validated(CreateGroup.class) Project request) {
    Project response = service.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(
      summary = "プロジェクト更新",
      description = "プロジェクトの更新を行います。プロジェクト名と説明および完了フラグの変更を行います。プロジェクトIDが存在しない場合はエラーを返します。",
      requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "更新したいプロジェクト",
          required = true,
          content = @Content(
              schema = @Schema(implementation = Project.class)
          )
      ),
      responses = {
          @ApiResponse(
              responseCode = "200", description = "更新成功",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = Project.class)
              )
          ),
          @ApiResponse(
              responseCode = "400", description = "入力値のバリデーションエラー",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          ),
          @ApiResponse(
              responseCode = "404", description = "指定された受講生IDが存在しないときのエラー",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ErrorResponse.class))
          )
      }
  )
  @PutMapping("/{id}")
  public ResponseEntity<String> update(
      @PathVariable @Positive int id,
      @RequestBody @Validated(UpdateGroup.class) Project request) {
    request.setId(id);
    service.update(request);
    return ResponseEntity.ok("更新成功");
  }
}
