INSERT INTO projects(title, description, is_finished) VALUES ('タスク管理アプリ開発','A社から受託した開発', false);
INSERT INTO projects(title, description, is_finished) VALUES ('Java Silver勉強',null, false);
INSERT INTO projects(title, description, is_finished) VALUES ('プロジェクトX','社外秘', true);

INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (1,'バックエンド開発',null,true);
INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (1,'フロントエンド開発','Reactで',false);
INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (2,'参考書','テキスト通読',true);

INSERT INTO work_sessions (task_id, started_at, type, created_at, updated_at)
VALUES (1,'2026-01-01 09:00:00', 'TIMER', '2026-01-01 09:00:00', '2026-01-01 09:00:00')
;-- task_id=1のminutesはnull
INSERT INTO work_sessions (task_id, minutes, started_at, ended_at, type, created_at, updated_at)
VALUES (4, 30, '2026-01-02 09:00:00', '2026-01-02 09:30:00', 'TIMER', '2026-01-02 09:00:00', '2026-01-02 09:30:00');
INSERT INTO work_sessions (task_id, minutes, started_at, ended_at, type, created_at, updated_at)
VALUES (4, 45, '2026-01-02 10:00:00', '2026-01-02 10:45:00', 'TIMER', '2026-01-02 10:00:00', '2026-01-02 10:45:00');
--id =2のワークセッションは存在しない
