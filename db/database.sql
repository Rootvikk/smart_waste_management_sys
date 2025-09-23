-- Create the database for the Smart Waste Management System
CREATE DATABASE IF NOT EXISTS waste_management;

-- Use the newly created database
USE waste_management;

-- Table to store user information (Citizens, Admins, Workers)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('Citizen', 'Admin', 'Worker') NOT NULL
);

-- Table to store waste reports submitted by citizens
CREATE TABLE IF NOT EXISTS reports (
    id INT AUTO_INCREMENT PRIMARY KEY,
    citizen_id INT NOT NULL,
    description TEXT NOT NULL,
    location VARCHAR(255) NOT NULL,
    status ENUM('Pending', 'In Progress', 'Completed') DEFAULT 'Pending',
    submission_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (citizen_id) REFERENCES users(id)
);

-- Table to store tasks assigned to workers by the admin
CREATE TABLE IF NOT EXISTS tasks (
    id INT AUTO_INCREMENT PRIMARY KEY,
    report_id INT NOT NULL,
    worker_id INT NOT NULL,
    status ENUM('Assigned', 'Completed') DEFAULT 'Assigned',
    assigned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_date TIMESTAMP NULL,
    FOREIGN KEY (report_id) REFERENCES reports(id),
    FOREIGN KEY (worker_id) REFERENCES users(id)
);

-- Insert some sample data for testing purposes

-- Admin User
INSERT INTO users (username, password, role) VALUES ('admin', 'admin123', 'Admin');

-- Worker User
INSERT INTO users (username, password, role) VALUES ('worker', 'worker123', 'Worker');

-- Citizen User
INSERT INTO users (username, password, role) VALUES ('citizen', 'citizen123', 'Citizen');

-- Sample report from the citizen
INSERT INTO reports (citizen_id, description, location) VALUES (3, 'Overflowing garbage bin near the park entrance.', 'City Park, Main Street');
