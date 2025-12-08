-- Database and table for the To-Do app

CREATE DATABASE IF NOT EXISTS todo_db;
USE todo_db;

CREATE TABLE IF NOT EXISTS todos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    completed BOOLEAN DEFAULT FALSE
);

-- Optionally, insert some initial tasks for demo
INSERT INTO todos (description, completed) VALUES
  ('Try the app', FALSE),
  ('Mark something done', TRUE);