package loglens.engine;

import loglens.model.LogEntry;

// Callback interface for receiving events as log lines are processed in real-time.
//
// Why use an interface here?
// Using an interface decouples the watcher (which reads the file) from the presentation/output logic.
// The watcher doesn't care whether events are printed to the console, sent over WebSockets,
// saved to a database, or rendered in a GUI. Any component can implement LogListener
// to respond to these log events.
public interface LogListener {

    // Invoked whenever a log line is successfully parsed into a LogEntry
    void onEntry(LogEntry entry);

    // Invoked whenever an error spike threshold is crossed for a service
    void onSpike(String service, int errorCount);

    // Invoked whenever an unparseable or broken line is encountered
    void onMalformed(String reason, String rawLine);
}
