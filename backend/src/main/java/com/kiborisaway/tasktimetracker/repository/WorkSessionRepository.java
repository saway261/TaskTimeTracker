package com.kiborisaway.tasktimetracker.repository;

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
   * 指定したタスクに紐づく作業セッションの作業時間合計を取得します。
   *
   * @param taskId タスクのID
   * @return 作業時間合計(分)
   */
  @Select("""
      SELECT COALESCE(SUM(minutes), 0)
      FROM work_sessions
      WHERE task_id = #{taskId}
      """)
  int sumMinutesByTaskId(int taskId);

  /**
   * 指定したタスクグループ配下の作業セッションの作業時間合計を取得します。
   *
   * @param taskGroupId タスクグループのID
   * @return 作業時間合計(分)
   */
  @Select("""
      SELECT COALESCE(SUM(ws.minutes), 0)
      FROM work_sessions ws
      INNER JOIN tasks t
        ON ws.task_id = t.id
      WHERE t.task_group_id = #{taskGroupId}
      """)
  int sumMinutesByTaskGroupId(int taskGroupId);

  /**
   * 指定したプロジェクト配下の作業セッションの作業時間合計を取得します。
   *
   * @param projectId プロジェクトのID
   * @return 作業時間合計(分)
   */
  @Select("""
      SELECT COALESCE(SUM(ws.minutes), 0)
      FROM work_sessions ws
      INNER JOIN tasks t
        ON ws.task_id = t.id
      WHERE t.project_id = #{projectId}
        OR t.task_group_id IN (
          SELECT id
          FROM task_groups
          WHERE project_id = #{projectId}
        )
      """)
  int sumMinutesByProjectId(int projectId);

  /**
   * 指定したタスクに紐づく作業セッション一覧を取得します。
   *
   * @param taskId タスクのID
   * @return 作業セッション一覧
   */
  @Select("""
      SELECT *
      FROM work_sessions
      WHERE task_id = #{taskId}
      ORDER BY id
      """)
  List<WorkSession> findAllByTaskId(int taskId);

  /**
   * IDを指定して作業セッションを取得します。
   *
   * @param id 作業セッションのID
   * @return 作業セッション
   */
  @Select("SELECT * FROM work_sessions WHERE id = #{id}")
  WorkSession findById(int id);

  /**
   * 作業セッションIDを指定して紐づくタスクIDを取得します。
   *
   * @param id 作業セッションのID
   * @return タスクのID。存在しなければnull
   */
  @Select("SELECT task_id FROM work_sessions WHERE id = #{id}")
  Integer findTaskIdById(int id);

  /**
   * タスクIDに紐づく作業セッションの存在チェックを行います
   *
   * @param taskId タスクのID
   * @return 存在すればtrue, 存在しなければfalse
   */
  @Select("""
      SELECT EXISTS(
        SELECT 1
        FROM work_sessions
        WHERE task_id = #{taskId}
      )
      """)
  boolean existsByTaskId(int taskId);

  /**
   * 指定したタスクに紐づく未終了の作業セッションが存在するかチェックします。
   *
   * @param taskId タスクのID
   * @return 未終了の作業セッションが存在すればtrue, 存在しなければfalse
   */
  @Select("""
      SELECT EXISTS(
        SELECT 1
        FROM work_sessions
        WHERE task_id = #{taskId}
          AND minutes IS NULL
      )
      """)
  boolean existsUnfinishedByTaskId(int taskId);

  /**
   * 指定したタスクに紐づく作業セッションを新規登録します。
   *
   * @param workSession 作業セッション
   */
  @Insert("""
      INSERT INTO work_sessions(task_id, minutes, started_at, type, created_at, updated_at)
      VALUES(
        #{taskId},
        #{minutes},
        CASE WHEN #{type} = 'TIMER' THEN NOW() ELSE NULL END,
        #{type},
        NOW(),
        NOW()
      )
      """)
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(WorkSession workSession);

  /**
   * 指定した作業セッションが終了日時を設定可能か判定します。
   *
   * @param wsId 作業セッションのID
   * @return 設定可能ならtrue, 設定不可ならfalse
   */
  @Select("""
      SELECT EXISTS(
        SELECT 1
        FROM work_sessions
        WHERE id = #{wsId}
          AND type = 'TIMER'
          AND started_at IS NOT NULL
          AND ended_at IS NULL
      )
      """)
  boolean canSetEnd(int wsId);

  /**
   * 指定した作業セッションに終了日時を設定します。 また、開始日時との差からminutesを計算してセットします。秒数は切り捨てます。
   *
   * @param wsId 作業セッションのID
   * @return 更新を実行した件数
   */
  @Update("""
      UPDATE work_sessions
      SET ended_at = LOCALTIMESTAMP,
          minutes = CAST(FLOOR(EXTRACT(EPOCH FROM (LOCALTIMESTAMP - started_at)) / 60)
              AS INTEGER),
          updated_at = LOCALTIMESTAMP
      WHERE id = #{wsId}
        AND type = 'TIMER'
        AND started_at IS NOT NULL
        AND ended_at IS NULL
      """)
  int setEnd(int wsId);

  /**
   * 作業セッションの作業時間、開始日時、終了日時を更新します。
   *
   * @param workSession 作業セッション
   * @return 更新を実行した件数
   */
  @Update("""
      UPDATE work_sessions
      SET minutes = CASE
              WHEN #{startedAt} IS NOT NULL AND #{endedAt} IS NOT NULL THEN
                CAST(FLOOR(EXTRACT(EPOCH FROM (
                    CAST(#{endedAt} AS TIMESTAMP) - CAST(#{startedAt} AS TIMESTAMP)
                )) / 60) AS INTEGER)
              ELSE
                #{minutes}
          END,
          started_at = #{startedAt},
          ended_at = #{endedAt},
          updated_at = LOCALTIMESTAMP
      WHERE id = #{id}
      """)
  int update(WorkSession workSession);

  /**
   * IDを指定して作業セッションを削除します。
   *
   * @param id 作業セッションのID
   * @return 削除を実行した件数
   */
  @Delete("DELETE FROM work_sessions WHERE id = #{id}")
  int deleteById(int id);

  /**
   * 指定したタスクに紐づく作業セッションをすべて削除します。
   *
   * @param taskId タスクのID
   * @return 削除を実行した件数
   */
  @Delete("DELETE FROM work_sessions WHERE task_id = #{taskId}")
  int deleteAllByTaskId(int taskId);

}
