package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.entity.EmailVerificationToken;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EmailVerificationTokenRepository {

  @Insert("""
      INSERT INTO email_verification_tokens(user_id, token_hash, expires_at, created_at)
      VALUES (#{userId}, #{tokenHash}, #{expiresAt}, #{createdAt})
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(EmailVerificationToken token);

  @Select("""
      SELECT * FROM email_verification_tokens
      WHERE token_hash = #{tokenHash}
      FOR UPDATE
      """)
  EmailVerificationToken findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

  @Update("""
      UPDATE email_verification_tokens
      SET used_at = #{usedAt}
      WHERE user_id = #{userId} AND used_at IS NULL
      """)
  int invalidateAllForUser(@Param("userId") int userId, @Param("usedAt") LocalDateTime usedAt);
}
