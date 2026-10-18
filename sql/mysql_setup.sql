-- =====================================================================
-- LogLens MySQL Setup Script: Schema & Stored Procedures
-- This script creates the demo schema and stored procedures used by
-- CallableDemo.java. It can be run directly in MySQL Workbench or CLI.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS loglens_demo;
USE loglens_demo;

-- 1. Incidents Table Definition
CREATE TABLE IF NOT EXISTS incidents (
    id INT PRIMARY KEY AUTO_INCREMENT,
    service VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL
);

-- =====================================================================
-- Stored Procedures
-- =====================================================================

-- Procedure a: incident_count_by_status
-- Demonstrates IN parameter (p_status) and OUT parameter (p_count)
DROP PROCEDURE IF EXISTS incident_count_by_status;
DELIMITER //
CREATE PROCEDURE incident_count_by_status(
    IN p_status VARCHAR(20),
    OUT p_count INT
)
BEGIN
    SELECT COUNT(*) INTO p_count
    FROM incidents
    WHERE status = p_status;
END //
DELIMITER ;

-- Procedure b: incidents_by_severity
-- Demonstrates IN parameter (p_severity) returning a ResultSet
DROP PROCEDURE IF EXISTS incidents_by_severity;
DELIMITER //
CREATE PROCEDURE incidents_by_severity(
    IN p_severity VARCHAR(20)
)
BEGIN
    SELECT id, service, severity, status
    FROM incidents
    WHERE severity = p_severity
    ORDER BY id ASC;
END //
DELIMITER ;

-- Procedure c: bump_severity
-- Demonstrates INOUT parameter (p_severity)
-- Receives a severity value, mutates it, and returns the escalated value
DROP PROCEDURE IF EXISTS bump_severity;
DELIMITER //
CREATE PROCEDURE bump_severity(
    INOUT p_severity VARCHAR(20)
)
BEGIN
    IF p_severity = 'MEDIUM' THEN
        SET p_severity = 'HIGH';
    ELSEIF p_severity = 'HIGH' THEN
        SET p_severity = 'CRITICAL';
    END IF;
    -- If already 'CRITICAL' or unknown, leave it unchanged
END //
DELIMITER ;
