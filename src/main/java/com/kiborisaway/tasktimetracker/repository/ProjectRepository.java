package com.kiborisaway.tasktimetracker.repository;

import com.kiborisaway.tasktimetracker.data.Project;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProjectRepository {

  /**
   * プロジェクトの全件検索を行います。
   *
   * @return プロジェクト一覧
   */
  @Select("SELECT * FROM projects")
  List<Project> findAll();

  /**
   * 取り組み中のプロジェクトのみを検索します。
   *
   * @return 取り組み中のプロジェクト一覧
   */
  @Select("SELECT * FROM projects WHERE is_finished=false")
  List<Project> findAllInProgress();

  /**
   * IDによるプロジェクトの単一検索を行います
   *
   * @param id プロジェクトのID
   * @return プロジェクト
   */
  @Select("SELECT * FROM projects WHERE id=#{id}")
  Project findById(int id);

  /**
   * プロジェクトの新規追加を行います。 完了フラグは新規追加時にはfalseとなります。
   *
   * @param project プロジェクト
   */
  @Insert("INSERT INTO projects(title, description, is_finished) VALUES(#{title}, #{description}, false)")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  void insert(Project project);

  /**
   * プロジェクトの更新を行います。プロジェクト名と説明と完了フラグを変更できます。 未変更の項目はDBに既存の値のままフロントエンドから返される想定で、全体更新します。
   *
   * @return 更新を実行した件数
   */
  @Update(
      "UPDATE projects SET title=#{title}, description=#{description}, is_finished=#{isFinished} "
          + "WHERE id=#{id}")
  int update(Project project);

}
