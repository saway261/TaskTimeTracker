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
   * @return プロジェクト一覧
   */
  @Select("SELECT * FROM projects")
  List<Project> searchProjectList();

  /**
   * IDによるプロジェクトの単一検索を行います
   * @param id プロジェクトのID
   * @return プロジェクト
   */
  @Select("SELECT * FROM projects WHERE id=#{id}")
  Project searchProjectById(int id);

  /**
   * プロジェクトの新規追加を行います。
   * @param project プロジェクト
   */
  @Insert("INSERT INTO projects(title, description) VALUES(#{title}, #{description})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  void registerProject(Project project);

  /**
   * プロジェクト名と説明の更新を行います。
   * 未変更の項目はDBに既存の値のままフロントエンドから返される想定で、全体更新します。
   * @return 更新を実行した件数
   */
  @Update("UPDATE projects SET title=#{title}, description=#{description} "
      + "WHERE id=#{id}")
  int updateProject(Project project);

}
