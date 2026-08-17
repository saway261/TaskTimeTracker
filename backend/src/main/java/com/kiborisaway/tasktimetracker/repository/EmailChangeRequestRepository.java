package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.entity.EmailChangeRequest;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface EmailChangeRequestRepository {

  @Insert("""
      INSERT INTO email_change_requests(user_id, new_email, token_hash, expires_at, created_at)
      VALUES (#{userId}, #{newEmail}, #{tokenHash}, #{expiresAt}, #{createdAt})
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(EmailChangeRequest request);

  @Select("""
      SELECT * FROM email_change_requests
      WHERE token_hash = #{tokenHash} AND used_at IS NULL AND expires_at > #{now}
      FOR UPDATE
      """)
  EmailChangeRequest findValidForUpdate(
      @Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

  @Update("""
      UPDATE email_change_requests
      SET used_at = #{usedAt}
      WHERE id = #{id} AND used_at IS NULL
      """)
  int markUsed(@Param("id") int id, @Param("usedAt") LocalDateTime usedAt);

  @Update("""
      UPDATE email_change_requests
      SET used_at = #{usedAt}
      WHERE user_id = #{userId} AND used_at IS NULL
      """)
  int invalidateAllForUser(@Param("userId") int userId, @Param("usedAt") LocalDateTime usedAt);

  @Delete("""
      DELETE FROM email_change_requests
      WHERE expires_at < #{threshold} OR (used_at IS NOT NULL AND used_at < #{threshold})
      """)
  int deleteExpiredOrUsed(@Param("threshold") LocalDateTime threshold);
}
