-- Database and table for the To-Do app (updated with last_modified)

CREATE DATABASE IF NOT EXISTS todo_db;
USE todo_db;

CREATE TABLE IF NOT EXISTS todos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    completed BOOLEAN DEFAULT FALSE,
    last_modified TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Optionally, insert some initial tasks for demo with sample timestamps
INSERT INTO todos (description, completed) VALUES
  ('Try the app', FALSE),
  ('Mark something done', TRUE);