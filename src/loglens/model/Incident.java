package loglens.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Represents an operational incident tracked in the database.
// An incident tracks recurring error patterns, severity, current lifecycle status, and ownership.
public class Incident {

    // Lifecycle status constants
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_ACKNOWLEDGED = "ACKNOWLEDGED";
    public static final String STATUS_RESOLVED = "RESOLVED";

    // Formatter used for readable timestamps in toString()
    private static final DateTimeFormatter FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int id;
    private String service;
    private String pattern;
    private String severity;
    private String status;
    private LocalDateTime openedAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private String handledBy;

    // Constructor for creating a brand NEW incident (sets status to OPEN)
    public Incident(String service, String pattern, String severity, LocalDateTime openedAt) {
        this.service = service;
        this.pattern = pattern;
        this.severity = severity;
        this.openedAt = openedAt;
        this.status = STATUS_OPEN;
    }

    // Overloaded constructor for restoring an incident loaded from the database
    public Incident(int id, String service, String pattern, String severity, String status, 
                    LocalDateTime openedAt, LocalDateTime acknowledgedAt, 
                    LocalDateTime resolvedAt, String handledBy) {
        this.id = id;
        this.service = service;
        this.pattern = pattern;
        this.severity = severity;
        this.status = status;
        this.openedAt = openedAt;
        this.acknowledgedAt = acknowledgedAt;
        this.resolvedAt = resolvedAt;
        this.handledBy = handledBy;
    }

    // Getters for all fields
    public int getId() {
        return id;
    }

    public String getService() {
        return service;
    }

    public String getPattern() {
        return pattern;
    }

    public String getSeverity() {
        return severity;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public String getHandledBy() {
        return handledBy;
    }

    // Setters for mutable fields
    public void setId(int id) {
        this.id = id;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public void setHandledBy(String handledBy) {
        this.handledBy = handledBy;
    }

    // Returns a readable single-line summary of the incident
    @Override
    public String toString() {
        String openedStr = (openedAt != null) ? openedAt.format(FORMATTER) : "N/A";
        String handlerStr = (handledBy != null) ? " (handled by: " + handledBy + ")" : "";
        return String.format("[#%d] [%s] [%s] %s : %s (opened: %s)%s", 
                id, status, severity, service, pattern, openedStr, handlerStr);
    }
}
