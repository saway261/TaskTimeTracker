package com.kiborisaway.tasktimetracker.exception.handler;

import com.kiborisaway.tasktimetracker.exception.EstimateMinutesUpdateNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.EmailUnavailableException;
import com.kiborisaway.tasktimetracker.exception.InvalidItemOrderException;
import com.kiborisaway.tasktimetracker.exception.PasswordPolicyViolationException;
import com.kiborisaway.tasktimetracker.exception.PasswordChangeNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.ReflectionAlreadyExistsException;
import com.kiborisaway.tasktimetracker.exception.ReflectionOperationNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.TaskFinishNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.TargetNotFoundException;
import com.kiborisaway.tasktimetracker.exception.WorkSessionEndNotAllowedException;
import com.kiborisaway.tasktimetracker.exception.WorkSessionOperationNotAllowedException;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String FOREIGN_KEY_VIOLATION = "23503";
  private static final String H2_PARENT_MISSING = "23506";
  private static final String UNIQUE_VIOLATION = "23505";

  ErrorDetailsBuilder errorDetailsBuilder;

  @Autowired
  public GlobalExceptionHandler(ErrorDetailsBuilder errorDetailsBuilder) {
    this.errorDetailsBuilder = errorDetailsBuilder;
  }

  /**
   * バリデーションエラーの発生をクライアントに返します。
   *
   * @param ex MethodArgumentNotValidException
   * @return HTTPステータス(BAD REQUEST), エラー詳細
   */
  @org.springframework.web.bind.annotation.ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex) {

    ErrorResponse errorResponse =
        new ErrorResponse(HttpStatus.BAD_REQUEST, "payload validation error",
            errorDetailsBuilder.buildErrorDetails(ex));
    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * JSONリクエストボディをDTOへ変換できなかったことをクライアントに返します。
   *
   * @param ex HttpMessageNotReadableException
   * @return HTTPステータス(BAD REQUEST), エラー詳細
   */
  @org.springframework.web.bind.annotation.ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex) {
    String field = findUnreadableField(ex);
    Map<String, String> error = Map.of(
        "field", field,
        "message", "JSONの形式または値が不正です");
    ErrorResponse errorResponse = new ErrorResponse(
        HttpStatus.BAD_REQUEST, "payload format error", List.of(error));
    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * バリデーションエラー(引数の型不一致)の発生をクライアントに返します。
   *
   * @param ex MethodArgumentTypeMismatchException
   * @return HTTPステータス(BAD REQUEST), エラー詳細
   */
  @org.springframework.web.bind.annotation.ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException ex) {

    ErrorResponse errorResponse =
        new ErrorResponse(HttpStatus.BAD_REQUEST, "type mismatch error",
            errorDetailsBuilder.buildErrorDetails(ex));
    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * バリデーションエラーの発生をクライアントに返します。
   *
   * @param ex ConstraintViolationException
   * @return HTTPステータス(BAD REQUEST), エラー詳細
   */
  @org.springframework.web.bind.annotation.ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      ConstraintViolationException ex) {

    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST,
        "parameter validation error", errorDetailsBuilder.buildErrorDetails(ex));
    return ResponseEntity.badRequest().body(errorResponse);
  }

  /**
   * 検索及び更新処理において studentId, studentCourseId, courseCode
   * 等の識別子で指定するレコードが見つからなかったこと（nullだった場合を含む）をクライアントに返します。
   *
   * @param ex TargetNotFoundException
   * @return HTTPステータス(NOT_FOUND), エラー詳細
   */
  @org.springframework.web.bind.annotation.ExceptionHandler(TargetNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleUpdateTargetNotFoundException(
      TargetNotFoundException ex) {

    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND,
        "target not found", errorDetailsBuilder.buildErrorDetails(ex));
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

  }

  /**
   * 作業セッションが存在する、または完了済みのタスクの見積もり作業時間を変更できないことをクライアントに返します。
   *
   * @param ex EstimateMinutesUpdateNotAllowedException
   * @return HTTPステータス(BAD REQUEST), エラー詳細
   */
  @org.springframework.web.bind.annotation.ExceptionHandler(
      EstimateMinutesUpdateNotAllowedException.class)
  public ResponseEntity<ErrorResponse> handleEstimateMinutesUpdateNotAllowedException(
      EstimateMinutesUpdateNotAllowedException ex) {

    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST,
        "estimate minutes update not allowed", errorDetailsBuilder.buildErrorDetails(ex));
    return ResponseEntity.badRequest().body(errorResponse);

  }

  /**
   * 終了できない作業セッションへの終了操作をクライアントに返します。
   *
   * @param ex WorkSessionEndNotAllowedException
   * @return HTTPステータス(BAD_REQUEST), エラー詳細
   */
  @org.springframework.web.bind.annotation.ExceptionHandler(WorkSessionEndNotAllowedException.class)
  public ResponseEntity<ErrorResponse> handleWorkSessionEndNotAllowedException(
      WorkSessionEndNotAllowedException ex) {

    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST,
        "work session end not allowed", errorDetailsBuilder.buildErrorDetails(ex));
    return ResponseEntity.badRequest().body(errorResponse);

  }

  /**
   * 作業セッション操作不可をクライアントに返します。
   *
   * @param ex WorkSessionOperationNotAllowedException
   * @return HTTPステータス(BAD_REQUEST), エラー詳細
   */
  @org.springframework.web.bind.annotation.ExceptionHandler(
      WorkSessionOperationNotAllowedException.class)
  public ResponseEntity<ErrorResponse> handleWorkSessionOperationNotAllowedException(
      WorkSessionOperationNotAllowedException ex) {

    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST,
        "work session operation not allowed", errorDetailsBuilder.buildErrorDetails(ex));
    return ResponseEntity.badRequest().body(errorResponse);

  }

  /**
   * 未終了の作業セッションがあるタスクの完了不可をクライアントに返します。
   *
   * @param ex TaskFinishNotAllowedException
   * @return HTTPステータス(BAD_REQUEST), エラー詳細
   */
  @org.springframework.web.bind.annotation.ExceptionHandler(TaskFinishNotAllowedException.class)
  public ResponseEntity<ErrorResponse> handleTaskFinishNotAllowedException(
      TaskFinishNotAllowedException ex) {

    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST,
        "task finish not allowed", errorDetailsBuilder.buildErrorDetails(ex));
    return ResponseEntity.badRequest().body(errorResponse);

  }

  /**
   * 並び替えリクエストの項目が現在の並び順と過不足・重複していることをクライアントに返します。
   *
   * @param ex InvalidItemOrderException
   * @return HTTPステータス(BAD_REQUEST), エラー詳細
   */
  @org.springframework.web.bind.annotation.ExceptionHandler(InvalidItemOrderException.class)
  public ResponseEntity<ErrorResponse> handleInvalidItemOrderException(
      InvalidItemOrderException ex) {

    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST,
        "invalid item order", errorDetailsBuilder.buildErrorDetails(ex));
    return ResponseEntity.badRequest().body(errorResponse);

  }

  /**
   * 既に振り返りを持つタスクへの登録操作をクライアントに返します。
   *
   * @param ex ReflectionAlreadyExistsException
   * @return HTTPステータス(CONFLICT), エラー詳細
   */
  @org.springframework.web.bind.annotation.ExceptionHandler(ReflectionAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleReflectionAlreadyExistsException(
      ReflectionAlreadyExistsException ex) {
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT,
        "reflection already exists", errorDetailsBuilder.buildErrorDetails(ex));
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  /**
   * 未完了タスクへの振り返り操作をクライアントに返します。
   *
   * @param ex ReflectionOperationNotAllowedException
   * @return HTTPステータス(CONFLICT), エラー詳細
   */
  @org.springframework.web.bind.annotation.ExceptionHandler(
      ReflectionOperationNotAllowedException.class)
  public ResponseEntity<ErrorResponse> handleReflectionOperationNotAllowedException(
      ReflectionOperationNotAllowedException ex) {
    ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT,
        "reflection operation not allowed", errorDetailsBuilder.buildErrorDetails(ex));
    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(EmailUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleEmailUnavailableException(
      EmailUnavailableException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), List.of()));
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(PasswordPolicyViolationException.class)
  public ResponseEntity<ErrorResponse> handlePasswordPolicyViolationException(
      PasswordPolicyViolationException ex) {
    return ResponseEntity.badRequest()
        .body(new ErrorResponse(HttpStatus.BAD_REQUEST, "payload validation error", List.of(
            Map.of("field", "password", "message", "パスワードが要件を満たしていません"))));
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(
      PasswordChangeNotAllowedException.class)
  public ResponseEntity<ErrorResponse> handlePasswordChangeNotAllowedException(
      PasswordChangeNotAllowedException ex) {
    return ResponseEntity.badRequest()
        .body(new ErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of()));
  }

  /**
   * DBの整合性制約違反をSQLSTATEに応じたレスポンスへ変換します。
   *
   * @param ex DBの整合性制約違反
   * @return 外部キー・一意制約違反はCONFLICT、それ以外はINTERNAL_SERVER_ERROR
   */
  @org.springframework.web.bind.annotation.ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
      DataIntegrityViolationException ex) {
    String sqlState = findSqlState(ex);

    if (FOREIGN_KEY_VIOLATION.equals(sqlState) || H2_PARENT_MISSING.equals(sqlState)) {
      return buildDatabaseErrorResponse(HttpStatus.CONFLICT, "foreign key constraint violation");
    }
    if (UNIQUE_VIOLATION.equals(sqlState)) {
      return buildDatabaseErrorResponse(HttpStatus.CONFLICT, "unique constraint violation");
    }

    logger.error("Unhandled database integrity violation (SQLSTATE: {})", sqlState, ex);
    return buildDatabaseErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
        "internal server error");
  }

  private ResponseEntity<ErrorResponse> buildDatabaseErrorResponse(HttpStatus status,
      String message) {
    ErrorResponse errorResponse = new ErrorResponse(status, message, List.of());
    return ResponseEntity.status(status).body(errorResponse);
  }

  private String findSqlState(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof SQLException sqlException && sqlException.getSQLState() != null) {
        return sqlException.getSQLState();
      }
      Throwable cause = current.getCause();
      if (cause == current) {
        break;
      }
      current = cause;
    }
    return null;
  }

  private String findUnreadableField(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof JacksonException jacksonException) {
        for (JacksonException.Reference reference : jacksonException.getPath()) {
          if (reference.getPropertyName() != null) {
            return reference.getPropertyName();
          }
        }
      }
      if (current.getCause() == current) {
        break;
      }
      current = current.getCause();
    }
    return "requestBody";
  }

}
