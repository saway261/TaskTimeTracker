package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.dto.work_session.ActiveTimerResponse;
import com.kiborisaway.tasktimetracker.data.entity.WorkSession;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WorkSessionRepository {

  /**
   * ログインユーザーが所有する全タスクから、未終了タイマーを取得します。
   * タスク詳細への導線を組み立てられるよう親IDとタスク名も返します。
   */
  @Select("""
      SELECT ws.id AS session_id,
             t.id AS task_id,
             t.title AS task_title,
             p.id AS project_id,
             t.task_group_id,
             ws.started_at
      FROM work_sessions ws
      JOIN tasks t ON ws.task_id = t.id
      LEFT JOIN task_groups tg ON tg.id = t.task_group_id
      JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
      WHERE ws.type = 'TIMER'
        AND ws.ended_at IS NULL
        AND p.user_id = #{userId}
      ORDER BY ws.started_at, ws.id
      """)
  List<ActiveTimerResponse> findAllActiveByUserId(int userId);

  /**
   * 指定したタスクに紐づく作業セッションの作業時間合計を取得します。所有者が一致するプロジェクトのみを対象とします。
   *
   * @param taskId タスクのID
   * @return 作業時間合計(分)
   */
  @Select("""
      SELECT CAST(FLOOR(COALESCE(SUM(ws.duration_seconds), 0) / 60.0) AS INTEGER)
      FROM work_sessions ws
      JOIN tasks t ON ws.task_id = t.id
      LEFT JOIN task_groups tg ON tg.id = t.task_group_id
      JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
      WHERE ws.task_id = #{taskId} AND p.user_id = #{userId}
      """)
  int sumMinutesByTaskId(int taskId, int userId);

  /**
   * 指定したタスクグループ配下の作業セッションの作業時間合計を取得します。所有者が一致するプロジェクトのみを対象とします。
   *
   * @param taskGroupId タスクグループのID
   * @return 作業時間合計(分)
   */
  @Select("""
      SELECT CAST(FLOOR(COALESCE(SUM(ws.duration_seconds), 0) / 60.0) AS INTEGER)
      FROM work_sessions ws
      JOIN tasks t ON ws.task_id = t.id
      JOIN task_groups tg ON tg.id = t.task_group_id
      JOIN projects p ON p.id = tg.project_id
      WHERE t.task_group_id = #{taskGroupId} AND p.user_id = #{userId}
      """)
  int sumMinutesByTaskGroupId(int taskGroupId, int userId);

  /**
   * 指定したプロジェクト配下の作業セッションの作業時間合計を取得します。所有者が一致するプロジェクトのみを対象とします。
   *
   * @param projectId プロジェクトのID
   * @return 作業時間合計(分)
   */
  @Select("""
      SELECT CAST(FLOOR(COALESCE(SUM(ws.duration_seconds), 0) / 60.0) AS INTEGER)
      FROM work_sessions ws
      JOIN tasks t ON ws.task_id = t.id
      LEFT JOIN task_groups tg ON tg.id = t.task_group_id
      JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
      WHERE (
          t.project_id = #{projectId}
          OR t.task_group_id IN (
            SELECT id
            FROM task_groups
            WHERE project_id = #{projectId}
          )
        )
        AND p.user_id = #{userId}
      """)
  int sumMinutesByProjectId(int projectId, int userId);

  /**
   * 指定したタスクに紐づく作業セッション一覧を取得します。所有者が一致するプロジェクトのみを対象とします。
   *
   * @param taskId タスクのID
   * @return 作業セッション一覧
   */
  @Select("""
      SELECT ws.*,
        CASE WHEN ws.duration_seconds IS NULL THEN NULL
             ELSE CAST(FLOOR(ws.duration_seconds / 60.0) AS INTEGER)
        END AS minutes
      FROM work_sessions ws
      JOIN tasks t ON ws.task_id = t.id
      LEFT JOIN task_groups tg ON tg.id = t.task_group_id
      JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
      WHERE ws.task_id = #{taskId} AND p.user_id = #{userId}
      ORDER BY ws.id
      """)
  List<WorkSession> findAllByTaskId(int taskId, int userId);

  /**
   * IDを指定して作業セッションを取得します。所有者が一致するプロジェクトのみを対象とします。
   *
   * @param id 作業セッションのID
   * @return 作業セッション
   */
  @Select("""
      SELECT ws.*,
        CASE WHEN ws.duration_seconds IS NULL THEN NULL
             ELSE CAST(FLOOR(ws.duration_seconds / 60.0) AS INTEGER)
        END AS minutes
      FROM work_sessions ws
      JOIN tasks t ON ws.task_id = t.id
      LEFT JOIN task_groups tg ON tg.id = t.task_group_id
      JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
      WHERE ws.id = #{id} AND p.user_id = #{userId}
      """)
  WorkSession findById(int id, int userId);

  /**
   * 作業セッションIDを指定して紐づくタスクIDを取得します。所有者が一致するプロジェクトのみを対象とします。
   *
   * @param id 作業セッションのID
   * @return タスクのID。存在しなければnull
   */
  @Select("""
      SELECT ws.task_id FROM work_sessions ws
      JOIN tasks t ON ws.task_id = t.id
      LEFT JOIN task_groups tg ON tg.id = t.task_group_id
      JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
      WHERE ws.id = #{id} AND p.user_id = #{userId}
      """)
  Integer findTaskIdById(int id, int userId);

  /**
   * タスクIDに紐づく作業セッションの存在チェックを行います。所有者が一致しない場合は常にfalseを返します。
   *
   * @param taskId タスクのID
   * @return 存在すればtrue, 存在しなければfalse
   */
  @Select("""
      SELECT EXISTS(
        SELECT 1
        FROM work_sessions ws
        JOIN tasks t ON ws.task_id = t.id
        LEFT JOIN task_groups tg ON tg.id = t.task_group_id
        JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
        WHERE ws.task_id = #{taskId} AND p.user_id = #{userId}
      )
      """)
  boolean existsByTaskId(int taskId, int userId);

  /**
   * 指定したタスクに紐づく未終了の作業セッションが存在するかチェックします。所有者が一致しない場合は常にfalseを返します。
   *
   * @param taskId タスクのID
   * @return 未終了の作業セッションが存在すればtrue, 存在しなければfalse
   */
  @Select("""
      SELECT EXISTS(
        SELECT 1
        FROM work_sessions ws
        JOIN tasks t ON ws.task_id = t.id
        LEFT JOIN task_groups tg ON tg.id = t.task_group_id
        JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
        WHERE ws.task_id = #{taskId}
          AND ws.type = 'TIMER'
          AND ws.ended_at IS NULL
          AND p.user_id = #{userId}
      )
      """)
  boolean existsUnfinishedByTaskId(int taskId, int userId);

  /**
   * 指定したタスクに紐づく作業セッションを新規登録します。 親タスクの所有権はService層で事前に確認済みであることが前提です。
   *
   * @param workSession 作業セッション
   */
  @Insert("""
      INSERT INTO work_sessions(task_id, duration_seconds, started_at, type, created_at, updated_at)
      VALUES(
        #{taskId},
        CASE WHEN #{type} = 'MANUAL' THEN CAST(#{minutes} AS BIGINT) * 60 ELSE NULL END,
        CASE WHEN #{type} = 'TIMER' THEN NOW() ELSE NULL END,
        #{type},
        NOW(),
        NOW()
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(WorkSession workSession);

  /**
   * 指定した作業セッションが終了日時を設定可能か判定します。所有者が一致しない場合は常にfalseを返します。
   *
   * @param wsId 作業セッションのID
   * @return 設定可能ならtrue, 設定不可ならfalse
   */
  @Select("""
      SELECT EXISTS(
        SELECT 1
        FROM work_sessions ws
        JOIN tasks t ON ws.task_id = t.id
        LEFT JOIN task_groups tg ON tg.id = t.task_group_id
        JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
        WHERE ws.id = #{wsId}
          AND ws.type = 'TIMER'
          AND ws.started_at IS NOT NULL
          AND ws.ended_at IS NULL
          AND p.user_id = #{userId}
      )
      """)
  boolean canSetEnd(int wsId, int userId);

  /**
   * 指定した作業セッションに終了日時を設定します。 また、開始日時との差からdurationSecondsを計算してセットします。
   * 所有者が一致しない場合は更新されません。
   *
   * @param wsId 作業セッションのID
   * @return 更新を実行した件数
   */
  @Update("""
      UPDATE work_sessions
      SET ended_at = LOCALTIMESTAMP,
          duration_seconds = CAST(FLOOR(EXTRACT(EPOCH FROM (LOCALTIMESTAMP - started_at)))
              AS BIGINT),
          updated_at = LOCALTIMESTAMP
      WHERE id = #{wsId}
        AND type = 'TIMER'
        AND started_at IS NOT NULL
        AND ended_at IS NULL
        AND EXISTS (
          SELECT 1 FROM work_sessions ws
          JOIN tasks t ON ws.task_id = t.id
          LEFT JOIN task_groups tg ON tg.id = t.task_group_id
          JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
          WHERE ws.id = work_sessions.id AND p.user_id = #{userId}
        )
      """)
  int setEnd(int wsId, int userId);

  /**
   * 作業セッションの作業時間、開始日時、終了日時を更新します。所有者が一致しない場合は更新されません。
   *
   * @param workSession 作業セッション
   * @return 更新を実行した件数
   */
  @Update("""
      UPDATE work_sessions
      SET duration_seconds = CASE
              WHEN #{workSession.startedAt} IS NOT NULL AND #{workSession.endedAt} IS NOT NULL THEN
                CAST(FLOOR(EXTRACT(EPOCH FROM (
                    CAST(#{workSession.endedAt} AS TIMESTAMP) - CAST(#{workSession.startedAt} AS TIMESTAMP)
                ))) AS BIGINT)
              ELSE
                CAST(#{workSession.minutes} AS BIGINT) * 60
          END,
          started_at = #{workSession.startedAt},
          ended_at = #{workSession.endedAt},
          updated_at = LOCALTIMESTAMP
      WHERE id = #{workSession.id}
        AND EXISTS (
          SELECT 1 FROM work_sessions ws
          JOIN tasks t ON ws.task_id = t.id
          LEFT JOIN task_groups tg ON tg.id = t.task_group_id
          JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
          WHERE ws.id = work_sessions.id AND p.user_id = #{userId}
        )
      """)
  int update(WorkSession workSession, int userId);

  /**
   * IDを指定して作業セッションを削除します。所有者が一致しない場合は削除されません。
   *
   * @param id 作業セッションのID
   * @return 削除を実行した件数
   */
  @Delete("""
      DELETE FROM work_sessions
      WHERE id = #{id}
        AND EXISTS (
          SELECT 1 FROM work_sessions ws
          JOIN tasks t ON ws.task_id = t.id
          LEFT JOIN task_groups tg ON tg.id = t.task_group_id
          JOIN projects p ON p.id = COALESCE(t.project_id, tg.project_id)
          WHERE ws.id = work_sessions.id AND p.user_id = #{userId}
        )
      """)
  int deleteById(int id, int userId);

  /**
   * 指定したタスクに紐づく作業セッションをすべて削除します。 呼び出し前にタスクの所有権が検証済みであることが前提です。
   *
   * @param taskId タスクのID
   * @return 削除を実行した件数
   */
  @Delete("DELETE FROM work_sessions WHERE task_id = #{taskId}")
  int deleteAllByTaskId(int taskId);

}
