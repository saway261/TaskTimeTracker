CREATE TABLE IF NOT EXISTS projects (
id INT AUTO_INCREMENT PRIMARY KEY,
title VARCHAR(20) NOT NULL,
description VARCHAR(200),
CONSTRAINT chk_projects_title_not_blank
 CHECK (TRIM(title) <> '')
);

CREATE TABLE IF NOT EXISTS task_groups (
id INT AUTO_INCREMENT PRIMARY KEY,
project_id INT NOT NULL,
title VARCHAR(20) NOT NULL,
description VARCHAR(200),
display_order INT NOT NULL,
FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE IF NOT EXISTS tasks (
id INT AUTO_INCREMENT PRIMARY KEY,
project_id INT,
task_group_id INT,
title VARCHAR(20) NOT NULL,
description VARCHAR(200),
display_order INT NOT NULL,
created_at TIMESTAMP NOT NULL,
completed_at TIMESTAMP,
actual_minutes_cached INT,
gap_minutes_cached INT,
gap_rate_cached DOUBLE,
FOREIGN KEY (project_id) REFERENCES projects(id),
FOREIGN KEY (task_group_id) REFERENCES task_groups(id),
CONSTRAINT chk_tasks_parent_xor
    CHECK (
      (project_id IS NOT NULL AND task_group_id IS NULL)
      OR
      (project_id IS NULL AND task_group_id IS NOT NULL)
    )
);