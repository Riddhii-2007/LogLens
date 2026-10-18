package loglens.engine;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;

import loglens.dao.IncidentDAO;
import loglens.model.Incident;
import loglens.model.LogEntry;

// Intercepts log stream events to automatically open, track, and escalate incidents in the database.
//
// Why does IncidentManager implement LogListener?
// By implementing LogListener, IncidentManager fits directly into the existing listener pipeline
// (Decorator / Pipeline pattern). LogWatcher doesn't need to know anything about databases or incidents;
// it simply notifies its listener. IncidentManager performs incident creation and forwards events to
// the 'next' downstream listener (e.g., console printer or dashboard).
public class IncidentManager implements LogListener {

    private final IncidentDAO dao;
    private final ErrorGrouper grouper;
    private final SpikeDetector detector;
    private final LogListener next;

    // In-memory lookup tables for active incidents tracked by service name
    private final HashMap<String, Integer> activeIncidentIds;
    private final HashMap<String, String> activeSeverities;

    // Constructor: receives the DAO, ErrorGrouper, SpikeDetector, and an optional downstream listener (may be null)
    public IncidentManager(IncidentDAO dao, ErrorGrouper grouper, SpikeDetector detector, LogListener next) {
        this.dao = dao;
        this.grouper = grouper;
        this.detector = detector;
        this.next = next;
        this.activeIncidentIds = new HashMap<String, Integer>();
        this.activeSeverities = new HashMap<String, String>();
    }

    // Records incoming log entries into ErrorGrouper and checks if an ongoing incident should escalate.
    // Synchronized because both the watcher thread (onEntry/onSpike) and UI thread (incidentClosed) access the maps.
    @Override
    public synchronized void onEntry(LogEntry entry) {
        grouper.record(entry);

        // Check for severity escalation on error entries
        if (entry != null && entry.isError()) {
            String service = entry.getService();
            if (activeIncidentIds.containsKey(service)) {
                int count = detector.getErrorCount(service);

                // Determine target severity based on error volume in the current window
                String targetSeverity;
                if (count >= 10) {
                    targetSeverity = "CRITICAL";
                } else if (count >= 5) {
                    targetSeverity = "HIGH";
                } else {
                    targetSeverity = "MEDIUM";
                }

                String currentSeverity = activeSeverities.get(service);

                // Why does severity escalate and why does it never go down?
                // An incident starts when an anomaly is first detected (MEDIUM). As errors rapidly accumulate,
                // the severity escalates (HIGH, CRITICAL) to reflect increasing customer impact.
                // It NEVER automatically downgrades even if errors temporarily slow down, because the peak severity
                // represents the maximum blast radius of the outage until an engineer inspects and resolves it.
                if (rank(targetSeverity) > rank(currentSeverity)) {
                    int incidentId = activeIncidentIds.get(service);
                    try {
                        dao.updateSeverity(incidentId, targetSeverity);
                        activeSeverities.put(service, targetSeverity);
                        System.out.println("INCIDENT #" + incidentId + " ESCALATED: " + service + " -> " + targetSeverity);
                    } catch (SQLException e) {
                        System.err.println("Database error escalating incident #" + incidentId + ": " + e.getMessage());
                    }
                }
            }
        }

        // Forward entry to downstream listener
        if (next != null) {
            next.onEntry(entry);
        }
    }

    // Handles spike alerts by opening a new incident in the database if one is not already active
    @Override
    public synchronized void onSpike(String service, int errorCount) {
        // Why is SQLException caught inside the watcher thread?
        // LogWatcher runs on a continuous background thread. If a database issue arises
        // (e.g. disk locked or network hiccup), throwing an unhandled exception would abruptly terminate
        // the watcher thread and stop all log monitoring. Catching SQLException ensures log ingestion continues.
        try {
            // Why does the duplicate check exist?
            // During an ongoing outage, a service might continue experiencing errors across multiple time windows.
            // Checking hasActiveIncident() ensures we alert on the root issue once without flooding the database
            // with duplicate open incident tickets for the same problem.
            if (!dao.hasActiveIncident(service)) {
                String severity;
                if (errorCount >= 10) {
                    severity = "CRITICAL";
                } else if (errorCount >= 5) {
                    severity = "HIGH";
                } else {
                    severity = "MEDIUM";
                }

                String pattern = grouper.getTopPatternForService(service);
                Incident incident = new Incident(service, pattern, severity, LocalDateTime.now());

                int id = dao.insert(incident);
                activeIncidentIds.put(service, id);
                activeSeverities.put(service, severity);

                System.out.println("INCIDENT #" + id + " OPENED: " + service + " [" + severity + "]");
            } else {
                System.out.println("Incident already active for " + service + ", no new one created");
            }
        } catch (SQLException e) {
            System.err.println("Database error handling incident for " + service + ": " + e.getMessage());
        }

        // Forward spike event to downstream listener
        if (next != null) {
            next.onSpike(service, errorCount);
        }
    }

    // Called when an incident is resolved by an operator in the UI.
    // Why is this synchronized and needed?
    // Both the background LogWatcher thread and the Swing UI thread access activeIncidentIds and activeSeverities.
    // Making this synchronized prevents race conditions. Removing the service frees it so that any
    // future spike for that service can properly create a fresh new incident.
    public synchronized void incidentClosed(String service) {
        if (service != null) {
            activeIncidentIds.remove(service);
            activeSeverities.remove(service);
        }
    }


    // Forwards malformed log lines to downstream listener
    @Override
    public void onMalformed(String reason, String rawLine) {
        if (next != null) {
            next.onMalformed(reason, rawLine);
        }
    }

    // Helper method to convert severity names into numerical ranks for comparison:
    // MEDIUM (1) < HIGH (2) < CRITICAL (3)
    private int rank(String severity) {
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            return 3;
        } else if ("HIGH".equalsIgnoreCase(severity)) {
            return 2;
        } else if ("MEDIUM".equalsIgnoreCase(severity)) {
            return 1;
        }
        return 0;
    }
}
