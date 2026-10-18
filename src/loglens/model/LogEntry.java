package loglens.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// This class represents a single parsed log line from a log file.
// It is designed to be an immutable model object where data cannot be changed after creation.
public class LogEntry {

    // Formatter used to display the date and time in the standard format: yyyy-MM-dd HH:mm:ss
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // The date and time when the log event occurred
    private final LocalDateTime timestamp;

    // The severity level of the log (for example: INFO, WARN, ERROR)
    private final String level;

    // The name of the microservice or component that produced this log (e.g., payment-service)
    private final String service;

    // The actual log message explaining what happened
    private final String message;

    // Constructor: takes all four pieces of log data and assigns them to the fields
    public LogEntry(LocalDateTime timestamp, String level, String service, String message) {
        this.timestamp = timestamp;
        this.level = level;
        this.service = service;
        this.message = message;
    }

    // Getter for the timestamp field
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // Getter for the log level field
    public String getLevel() {
        return level;
    }

    // Getter for the service name field
    public String getService() {
        return service;
    }

    // Getter for the log message field
    public String getMessage() {
        return message;
    }

    // Helper method to check if this log entry represents an error.
    // Using "ERROR".equalsIgnoreCase(level) prevents NullPointerException if level is ever null.
    public boolean isError() {
        return "ERROR".equalsIgnoreCase(level);
    }

    // Returns a readable single-line string representation of the log entry.
    // Example: 2026-10-18 14:03:24 [ERROR] payment-service : Timeout connecting to DB
    @Override
    public String toString() {
        String formattedTimestamp = (timestamp != null) ? timestamp.format(DATE_TIME_FORMATTER) : "null";
        return formattedTimestamp + " [" + level + "] " + service + " : " + message;
    }
}
