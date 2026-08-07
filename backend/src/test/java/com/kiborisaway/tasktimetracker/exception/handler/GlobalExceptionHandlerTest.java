package com.kiborisaway.tasktimetracker.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler sut =
      new GlobalExceptionHandler(mock(ErrorDetailsBuilder.class));

  @ParameterizedTest
  @ValueSource(strings = {"23503", "23506"})
  void DB制約違反_外部キー違反なら詳細を公開せず409を返すこと(String sqlState) {
    // Arrange
    DataIntegrityViolationException exception = createException(sqlState, "sensitive detail");

    // Act
    ResponseEntity<ErrorResponse> actual =
        sut.handleDataIntegrityViolationException(exception);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(actual.getBody()).isNotNull();
    assertThat(actual.getBody().getMessage()).isEqualTo("foreign key constraint violation");
    assertThat(actual.getBody().getErrors()).isEmpty();
    assertThat(actual.getBody().getMessage()).doesNotContain("sensitive detail");
  }

  @Test
  void DB制約違反_UNIQUE違反なら詳細を公開せず409を返すこと() {
    // Arrange
    DataIntegrityViolationException exception = createException("23505", "sensitive detail");

    // Act
    ResponseEntity<ErrorResponse> actual =
        sut.handleDataIntegrityViolationException(exception);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(actual.getBody()).isNotNull();
    assertThat(actual.getBody().getMessage()).isEqualTo("unique constraint violation");
    assertThat(actual.getBody().getErrors()).isEmpty();
    assertThat(actual.getBody().getMessage()).doesNotContain("sensitive detail");
  }

  @Test
  void DB制約違反_未知のSQLSTATEなら詳細を公開せず500を返すこと() {
    // Arrange
    DataIntegrityViolationException exception = createException("23514", "sensitive detail");

    // Act
    ResponseEntity<ErrorResponse> actual =
        sut.handleDataIntegrityViolationException(exception);

    // Assert
    assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(actual.getBody()).isNotNull();
    assertThat(actual.getBody().getMessage()).isEqualTo("internal server error");
    assertThat(actual.getBody().getErrors()).isEmpty();
    assertThat(actual.getBody().getMessage()).doesNotContain("sensitive detail");
  }

  private DataIntegrityViolationException createException(String sqlState, String detail) {
    SQLException sqlException = new SQLException(detail, sqlState);
    return new DataIntegrityViolationException("database operation failed", sqlException);
  }
}
