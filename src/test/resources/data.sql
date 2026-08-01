INSERT INTO projects(title, description, is_finished) VALUES ('タスク管理アプリ開発','A社から受託した開発', false);
INSERT INTO projects(title, description, is_finished) VALUES ('Java Silver勉強',null, false);
INSERT INTO projects(title, description, is_finished) VALUES ('プロジェクトX','社外秘', true);

INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (1,'バックエンド開発',null,true);
INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (1,'フロントエンド開発','Reactで',false);
INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (2,'参考書','テキスト通読',true);

INSERT INTO tasks (task_group_id, title, description, estimated_minutes, created_at)
VALUES (1, 'カスタム例外作成', 'カスタム例外クラスの作成とハンドリングを行う', 60, NOW());--id=1
INSERT INTO tasks (task_group_id, title, description, estimated_minutes, created_at)
VALUES (1, 'バリデーション実装', 'バリデーションの実装とバリデーション違反のハンドリングを行う', 120, NOW());-- id=2
INSERT INTO tasks (task_group_id, title, description, estimated_minutes, created_at, finished_at, actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
VALUES (2, 'コンポーネント作成', '共通UIコンポーネントを作成する', 90, NOW(), NOW(), 100, 10, 11.1111111111);-- id=3
INSERT INTO tasks (project_id, title, description, estimated_minutes, created_at)
VALUES (1, '画面設計', 'ワイヤーフレームを作成する', 180, NOW());-- id=4
INSERT INTO tasks (task_group_id, title, description, estimated_minutes, created_at)
VALUES (3, '章末問題', 'Java Silverの章末問題を解く', 150, NOW());-- id=5

INSERT INTO work_sessions (task_id, started_at, type, created_at, updated_at)
VALUES (1,'2026-01-01 09:00:00', 'TIMER', '2026-01-01 09:00:00', '2026-01-01 09:00:00')
;-- task_id=1のminutesはnull
INSERT INTO work_sessions (task_id, minutes, started_at, ended_at, type, created_at, updated_at)
VALUES (4, 30, '2026-01-02 09:00:00', '2026-01-02 09:30:00', 'TIMER', '2026-01-02 09:00:00', '2026-01-02 09:30:00');
INSERT INTO work_sessions (task_id, minutes, started_at, ended_at, type, created_at, updated_at)
VALUES (4, 45, '2026-01-02 10:00:00', '2026-01-02 10:45:00', 'TIMER', '2026-01-02 10:00:00', '2026-01-02 10:45:00');
--id =2のワークセッションは存在しない
