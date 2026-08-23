package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.entity.AppUser;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserRepository {

  @Select("SELECT * FROM app_users WHERE email = #{email}")
  AppUser findByEmail(String email);

  @Select("SELECT * FROM app_users WHERE id = #{id}")
  AppUser findById(int id);

  @Select("SELECT EXISTS(SELECT 1 FROM app_users WHERE email = #{email})")
  boolean existsByEmail(String email);

  @Insert("""
      INSERT INTO app_users(
        email, password_hash, is_enabled, password_change_required,
        temporary_password_expires_at, created_at, updated_at
      ) VALUES (
        #{email}, #{passwordHash}, #{isEnabled}, #{passwordChangeRequired},
        #{temporaryPasswordExpiresAt}, #{createdAt}, #{updatedAt}
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(AppUser user);

  @Update("""
      UPDATE app_users
      SET password_hash = #{passwordHash},
          password_change_required = FALSE,
          temporary_password_expires_at = NULL,
          updated_at = #{updatedAt}
      WHERE id = #{userId}
      """)
  int updatePassword(
      @Param("userId") int userId,
      @Param("passwordHash") String passwordHash,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Update("""
      UPDATE app_users
      SET password_hash = #{passwordHash},
          password_change_required = TRUE,
          temporary_password_expires_at = #{expiresAt},
          updated_at = #{updatedAt}
      WHERE id = #{userId}
      """)
  int updateTemporaryPassword(
      @Param("userId") int userId,
      @Param("passwordHash") String passwordHash,
      @Param("expiresAt") LocalDateTime expiresAt,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Update("""
      UPDATE app_users
      SET email_verified_at = #{verifiedAt},
          updated_at = #{updatedAt}
      WHERE id = #{userId}
      """)
  int updateEmailVerified(
      @Param("userId") int userId,
      @Param("verifiedAt") LocalDateTime verifiedAt,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Update("""
      UPDATE app_users
      SET email = #{email},
          email_verified_at = #{verifiedAt},
          updated_at = #{updatedAt}
      WHERE id = #{userId}
      """)
  int updateEmail(
      @Param("userId") int userId,
      @Param("email") String email,
      @Param("verifiedAt") LocalDateTime verifiedAt,
      @Param("updatedAt") LocalDateTime updatedAt);

  @Update("""
      UPDATE app_users
      SET onboarding_completed = TRUE,
          updated_at = #{updatedAt}
      WHERE id = #{userId}
      """)
  int updateOnboardingCompleted(
      @Param("userId") int userId,
      @Param("updatedAt") LocalDateTime updatedAt);
}
