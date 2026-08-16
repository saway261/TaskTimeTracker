package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.entity.PasswordResetToken;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PasswordResetTokenRepository {

  @Insert("""
      INSERT INTO password_reset_tokens(user_id, token_hash, expires_at, created_at)
      VALUES (#{userId}, #{tokenHash}, #{expiresAt}, #{createdAt})
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(PasswordResetToken token);

  @Select("""
      SELECT * FROM password_reset_tokens
      WHERE token_hash = #{tokenHash} AND used_at IS NULL AND expires_at > #{now}
      FOR UPDATE
      """)
  PasswordResetToken findValidForUpdate(
      @Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

  @Update("""
      UPDATE password_reset_tokens
      SET used_at = #{usedAt}
      WHERE user_id = #{userId} AND used_at IS NULL
      """)
  int invalidateAllForUser(@Param("userId") int userId, @Param("usedAt") LocalDateTime usedAt);
}
