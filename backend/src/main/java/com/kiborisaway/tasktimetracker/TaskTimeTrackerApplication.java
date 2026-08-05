package com.kiborisaway.tasktimetracker;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(
    title = "タスク見積精度改善アプリ TaskTimeTracker",
    description = """
        タスクに対して見積作業時間と実績作業時間を管理し、タスクに対して要する時間の見積もり予測精度の改善に役立てます。
        タスクをプロジェクトとタスクグループによってグルーピングし、連続性のあるタスクを管理することができます。
        見積作業時間と実績作業時間の差と比率に対して必ず反省を行います。
        """,
    version = "1.0"))
@SpringBootApplication
public class TaskTimeTrackerApplication {

  public static void main(String[] args) {
    SpringApplication.run(TaskTimeTrackerApplication.class, args);
  }

}
