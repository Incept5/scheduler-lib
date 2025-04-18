-- If you want to use this file, you need to add the following line to your application.yaml:
-- quarkus.flyway.locations=/db/migration,/scheduler-migration
-- Alternatively, you can copy this file into your service's resources/db/migration folder and rename it to V1__init_schedules.sql
CREATE SCHEMA IF NOT EXISTS ${flyway:defaultSchema};

CREATE TABLE IF NOT EXISTS ${flyway:defaultSchema}.scheduled_tasks (
  task_name text NOT NULL,
  task_instance text NOT NULL,
  task_data bytea,
  execution_time timestamp with time zone NOT NULL,
  picked BOOLEAN NOT NULL,
  picked_by text,
  last_success timestamp with time zone,
  last_failure timestamp with time zone,
  consecutive_failures INT,
  last_heartbeat timestamp with time zone,
  version BIGINT NOT NULL,
  PRIMARY KEY (task_name, task_instance)
);

CREATE INDEX execution_time_idx ON ${flyway:defaultSchema}.scheduled_tasks (execution_time);
CREATE INDEX last_heartbeat_idx ON ${flyway:defaultSchema}.scheduled_tasks (last_heartbeat);
