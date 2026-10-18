package loglens;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import loglens.model.LogEntry;
import loglens.exception.MalformedLogException;
import loglens.parser.LogParser;
import loglens.parser.AppLogParser;
import loglens.engine.SpikeDetector;

// Main entry point for the LogLens application.
// Demonstrates reading a log file line-by-line, parsing lines polymorphically,
// gracefully handling custom exceptions, detecting error bursts with a sliding window,
// and printing a summary of results.
public class Main {

    public static void main(String[] args) {
        // Name of the log file located in the project root
        String logFilePath = "sample.log";

        // Statistics counters for the summary report
        int totalLinesRead = 0;
        int parsedOkCount = 0;
        int malformedCount = 0;
        int errorEntriesCount = 0;
        int spikeAlertsCount = 0;

        // Polymorphism: we declare the variable using the abstract parent type LogParser,
        // but instantiate the concrete AppLogParser subclass.
        LogParser parser = new AppLogParser();

        // SpikeDetector: monitors error counts within a 10-second sliding window
        // and triggers an alert when a service reaches 3 or more errors.
        SpikeDetector detector = new SpikeDetector(10, 3);

        // Try-with-resources: automatically closes the BufferedReader and FileReader
        // when done, avoiding resource leaks even if an error occurs.
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;

            // Read the file line-by-line until end of file (readLine returns null)
            while ((line = reader.readLine()) != null) {
                totalLinesRead++;

                try {
                    // Attempt to parse the line into a LogEntry object
                    LogEntry entry = parser.parse(line);
                    System.out.println(entry);
                    parsedOkCount++;

                    // Record entry in SpikeDetector; returns true if a new spike threshold is crossed
                    if (detector.record(entry)) {
                        System.out.println("*** SPIKE ALERT: " + entry.getService() + " has " 
                                + detector.getErrorCount(entry.getService()) + " errors in the last 10 seconds ***");
                        spikeAlertsCount++;
                    }

                    // Count entries that have ERROR severity level
                    if (entry.isError()) {
                        errorEntriesCount++;
                    }
                } catch (MalformedLogException e) {
                    // When a line is malformed, print the reason and skip to the next line without crashing
                    System.out.println("SKIPPED (" + e.getMessage() + "): " + e.getRawLine());
                    malformedCount++;
                }
            }
        } catch (IOException e) {
            // Handle any file I/O issues (e.g., file not found or cannot be read)
            System.err.println("Error reading log file: " + e.getMessage());
        }

        // Print final summary report of the log processing
        System.out.println();
        System.out.println("=== Log Processing Summary ===");
        System.out.println("Total lines read: " + totalLinesRead);
        System.out.println("Parsed OK:        " + parsedOkCount);
        System.out.println("Malformed:        " + malformedCount);
        System.out.println("ERROR entries:    " + errorEntriesCount);
        System.out.println("Spike alerts:     " + spikeAlertsCount);
    }
}
