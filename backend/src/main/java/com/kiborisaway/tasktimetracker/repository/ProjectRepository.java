package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.entity.Project;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProjectRepository {

  /**
   * 認証ユーザーが所有するプロジェクトの全件検索を行います。
   *
   * @param userId 認証ユーザーのID
   * @return プロジェクト一覧
   */
  @Select("SELECT * FROM projects WHERE user_id=#{userId}")
  List<Project> findAllByUserId(int userId);

  /**
   * 認証ユーザーが所有するプロジェクトのうち、完了フラグを指定して検索します。
   *
   * @param isFinished 完了フラグ
   * @param userId     認証ユーザーのID
   * @return 指定した完了状態のプロジェクト一覧
   */
  @Select("SELECT * FROM projects WHERE is_finished=#{isFinished} AND user_id=#{userId}")
  List<Project> findAllByIsFinishedAndUserId(boolean isFinished, int userId);

  /**
   * IDと所有者による単一検索を行います。
   *
   * @param id     プロジェクトのID
   * @param userId 認証ユーザーのID
   * @return プロジェクト
   */
  @Select("SELECT * FROM projects WHERE id=#{id} AND user_id=#{userId}")
  Project findByIdAndUserId(int id, int userId);

  /**
   * IDと所有者によるプロジェクトの存在チェックを行います
   *
   * @param id     プロジェクトのID
   * @param userId 認証ユーザーのID
   * @return 存在すればtrue, 存在しなければfalse
   */
  @Select("""
      SELECT EXISTS(
        SELECT 1
        FROM projects
        WHERE id = #{id} AND user_id = #{userId}
      )
      """)
  boolean existsByIdAndUserId(int id, int userId);

  /**
   * プロジェクトの新規追加を行います。 完了フラグは新規追加時にはfalseとなります。所有者はProjectのuserIdに設定した値を用います。
   *
   * @param project プロジェクト
   */
  @Insert("INSERT INTO projects(user_id, title, description, is_finished) "
      + "VALUES(#{userId}, #{title}, #{description}, false)")
  @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
  void insert(Project project);

  /**
   * プロジェクトの更新を行います。プロジェクト名と説明を変更できます。 未変更の項目はDBに既存の値のままフロントエンドから返される想定で、全体更新します。
   * 所有者が一致しない場合は更新されません。
   *
   * @return 更新を実行した件数
   */
  @Update("UPDATE projects SET title=#{title}, description=#{description} "
      + "WHERE id=#{id} AND user_id=#{userId}")
  int update(Project project);

  /**
   * プロジェクトの完了状態を更新します。所有者が一致しない場合は更新されません。
   *
   * @param id         プロジェクトのID
   * @param isFinished 完了状態
   * @param userId     認証ユーザーのID
   * @return 更新を実行した件数
   */
  @Update("UPDATE projects SET is_finished=#{isFinished} WHERE id=#{id} AND user_id=#{userId}")
  int updateFinished(int id, boolean isFinished, int userId);

}
