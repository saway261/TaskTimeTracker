INSERT INTO app_users(
  email, password_hash, is_enabled, password_change_required,
  temporary_password_expires_at, created_at, updated_at, email_verified_at
) VALUES (
  'user-a@example.com',
  '{bcrypt}$2a$12$eXVmZkrzhJ4W18gvvloeBOtdTxZafS3hoF0JPQgGeoaJn4100Ss7u',
  TRUE, FALSE, NULL, '2026-08-13 00:00:00', '2026-08-13 00:00:00', '2026-08-13 00:00:00'
);
INSERT INTO app_users(
  email, password_hash, is_enabled, password_change_required,
  temporary_password_expires_at, created_at, updated_at, email_verified_at
) VALUES (
  'user-b@example.com',
  '{bcrypt}$2a$12$CcqglFlqf6yLLjmvKVE05uZ7XiphBYiNIw7eQ3Nr.H2ne0ehZSS0W',
  TRUE, FALSE, NULL, '2026-08-13 00:00:00', '2026-08-13 00:00:00', '2026-08-13 00:00:00'
);

INSERT INTO projects(user_id, title, description, is_finished) VALUES (1, 'タスク管理アプリ開発','A社から受託した開発', false);
INSERT INTO projects(user_id, title, description, is_finished) VALUES (1, 'Java Silver勉強',null, false);
INSERT INTO projects(user_id, title, description, is_finished) VALUES (2, 'プロジェクトX','社外秘', true);

INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (1,'バックエンド開発',null,true);
INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (1,'フロントエンド開発','Reactで',false);
INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (2,'参考書','テキスト通読',true);
INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (3,'振り返りグループA',null,false);
INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (3,'振り返りグループB',null,false);

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
INSERT INTO tasks (project_id, title, description, estimated_minutes, created_at, finished_at, actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
VALUES (3, '振り返り直下A', 'Reflectionありの完了タスク', 60, NOW(), '2026-08-10 10:00:00', 90, 30, 50.0);-- id=6
INSERT INTO tasks (project_id, title, description, estimated_minutes, created_at, finished_at, actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
VALUES (3, '振り返り直下B', 'Reflectionなしの完了タスク', 120, NOW(), '2026-08-11 11:00:00', 100, -20, -16.6666666667);-- id=7
INSERT INTO tasks (project_id, title, description, estimated_minutes, created_at)
VALUES (3, '振り返り直下未完了', '一覧対象外の未完了タスク', 30, NOW());-- id=8
INSERT INTO tasks (task_group_id, title, description, estimated_minutes, created_at, finished_at, actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
VALUES (4, '振り返り配下A', 'Reflectionありの完了タスク', 45, NOW(), '2026-08-12 12:00:00', 60, 15, 33.3333333333);-- id=9
INSERT INTO tasks (task_group_id, title, description, estimated_minutes, created_at, finished_at, actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
VALUES (4, '振り返り配下B', 'Reflectionなしの完了タスク', 90, NOW(), '2026-08-12 13:00:00', 90, 0, 0.0);-- id=10
INSERT INTO tasks (task_group_id, title, description, estimated_minutes, created_at)
VALUES (4, '振り返り配下未完了', '一覧対象外の未完了タスク', 30, NOW());-- id=11
INSERT INTO tasks (task_group_id, title, description, estimated_minutes, created_at, finished_at, actual_minutes_cached, gap_minutes_cached, gap_rate_cached)
VALUES (5, '振り返り別配下', 'グループ順確認用の完了タスク', 30, NOW(), '2026-08-12 14:00:00', 25, -5, -16.6666666667);-- id=12

INSERT INTO work_sessions (task_id, started_at, type, created_at, updated_at)
VALUES (1,'2026-01-01 09:00:00', 'TIMER', '2026-01-01 09:00:00', '2026-01-01 09:00:00')
;-- task_id=1のminutesはnull
INSERT INTO work_sessions (task_id, minutes, started_at, ended_at, type, created_at, updated_at)
VALUES (4, 30, '2026-01-02 09:00:00', '2026-01-02 09:30:00', 'TIMER', '2026-01-02 09:00:00', '2026-01-02 09:30:00');
INSERT INTO work_sessions (task_id, minutes, started_at, ended_at, type, created_at, updated_at)
VALUES (4, 45, '2026-01-02 10:00:00', '2026-01-02 10:45:00', 'TIMER', '2026-01-02 10:00:00', '2026-01-02 10:45:00');
--id =2のワークセッションは存在しない

INSERT INTO memos (project_id, comment)
VALUES (1, 'プロジェクト方針を確認する');-- id=1
INSERT INTO memos (task_group_id, comment)
VALUES (1, 'バックエンド優先で進める');-- id=2
INSERT INTO memos (task_id, comment)
VALUES (1, '例外メッセージを見直す');-- id=3

INSERT INTO reflections (task_id, cause, next_action, created_at, updated_at)
VALUES (6, '着手前の調査が不足していた', '類似タスクの実績を見積もり前に確認する', '2026-08-10 10:05:00', '2026-08-10 10:05:00');-- id=1
INSERT INTO reflections (task_id, cause, next_action, created_at, updated_at)
VALUES (9, 'レビュー観点に漏れがあった', NULL, '2026-08-12 12:05:00', '2026-08-12 12:05:00');-- id=2

INSERT INTO project_item_orders (project_id, task_id, task_group_id, position)
VALUES (3, 7, NULL, 0);
INSERT INTO project_item_orders (project_id, task_id, task_group_id, position)
VALUES (3, NULL, 5, 1);
INSERT INTO project_item_orders (project_id, task_id, task_group_id, position)
VALUES (3, 8, NULL, 2);
INSERT INTO project_item_orders (project_id, task_id, task_group_id, position)
VALUES (3, NULL, 4, 3);
INSERT INTO project_item_orders (project_id, task_id, task_group_id, position)
VALUES (3, 6, NULL, 4);

INSERT INTO task_group_item_orders (task_group_id, task_id, position)
VALUES (4, 10, 0);
INSERT INTO task_group_item_orders (task_group_id, task_id, position)
VALUES (4, 11, 1);
INSERT INTO task_group_item_orders (task_group_id, task_id, position)
VALUES (4, 9, 2);
INSERT INTO task_group_item_orders (task_group_id, task_id, position)
VALUES (5, 12, 0);
