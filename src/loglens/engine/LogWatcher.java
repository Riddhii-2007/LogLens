package loglens.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import loglens.model.LogEntry;
import loglens.parser.LogParser;
import loglens.exception.MalformedLogException;

// A background worker that continuously tails (watches) a log file in real-time,
// similar to the Linux 'tail -f' command.
//
// Why Runnable & Threads?
// Implementing Runnable allows LogWatcher to execute on a dedicated background thread.
// This frees up the main thread to perform other tasks (like generating new logs or handling user input)
// without freezing or blocking the application.
public class LogWatcher implements Runnable {

    private final String filePath;
    private final LogParser parser;
    private final SpikeDetector detector;
    private final LogListener listener;

    // Why 'volatile'?
    // In multi-threaded Java, threads may cache variables locally in CPU registers.
    // Marking 'running' as volatile guarantees that when the main thread calls stop()
    // and changes 'running' to false, the background watcher thread sees the updated value
    // immediately rather than looping infinitely on a stale cached copy.
    private volatile boolean running = true;

    // Counters to track statistics during execution
    private int linesRead;
    private int parsedOk;
    private int malformed;
    private int errorEntries;
    private int spikeAlerts;

    // Constructor: initializes all required collaborators
    public LogWatcher(String filePath, LogParser parser, SpikeDetector detector, LogListener listener) {
        this.filePath = filePath;
        this.parser = parser;
        this.detector = detector;
        this.listener = listener;
    }

    // Signals the background watcher loop to gracefully stop
    public void stop() {
        this.running = false;
    }

    // Main execution loop for the background thread
    @Override
    public void run() {
        File file = new File(filePath);

        // If the file does not exist yet when starting, wait and retry every 500 ms
        while (running && !file.exists()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        if (!running) {
            return;
        }

        // Open file in try-with-resources to ensure the reader is closed properly
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while (running) {
                String line = reader.readLine();

                if (line == null) {
                    // Why sleep when there is no new line?
                    // When reaching the end of the file, continuously calling readLine() in an empty loop
                    // would cause 100% CPU usage (busy-waiting). Sleeping for 500 ms gives the operating
                    // system time to breathe and allows writers to append new content.
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }

                linesRead++;

                try {
                    // Attempt to parse the line
                    LogEntry entry = parser.parse(line);
                    listener.onEntry(entry);
                    parsedOk++;

                    if (entry.isError()) {
                        errorEntries++;
                    }

                    // Check for an error spike
                    if (detector.record(entry)) {
                        String service = entry.getService();
                        listener.onSpike(service, detector.getErrorCount(service));
                        spikeAlerts++;
                    }
                } catch (MalformedLogException e) {
                    // Handle unparseable line
                    listener.onMalformed(e.getMessage(), e.getRawLine());
                    malformed++;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading log file in LogWatcher: " + e.getMessage());
        }
    }

    // Public getters for the summary counters
    public int getLinesRead() {
        return linesRead;
    }

    public int getParsedOk() {
        return parsedOk;
    }

    public int getMalformed() {
        return malformed;
    }

    public int getErrorEntries() {
        return errorEntries;
    }

    public int getSpikeAlerts() {
        return spikeAlerts;
    }
}
