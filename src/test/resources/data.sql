INSERT INTO projects(title, description, is_finished) VALUES ('タスク管理アプリ開発','A社から受託した開発', false);
INSERT INTO projects(title, description, is_finished) VALUES ('Java Silver勉強',null, false);
INSERT INTO projects(title, description, is_finished) VALUES ('プロジェクトX','社外秘', true);

INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (1,'バックエンド開発',null,true);
INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (1,'フロントエンド開発','Reactで',false);
INSERT INTO task_groups(project_id, title, description, is_finished) VALUES (2,'参考書','テキスト通読',true);
