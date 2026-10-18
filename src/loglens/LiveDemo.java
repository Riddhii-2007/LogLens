package loglens;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import loglens.model.LogEntry;
import loglens.parser.LogParser;
import loglens.parser.AppLogParser;
import loglens.engine.SpikeDetector;
import loglens.engine.LogListener;
import loglens.engine.LogWatcher;
import loglens.engine.ErrorGrouper;
import loglens.util.LogGenerator;

// Demonstrates real-time concurrent log processing.
// One thread (the background LogWatcher) tails and analyzes the log file,
// while another thread (the main thread) simulates an active application writing new logs.
public class LiveDemo {

    public static void main(String[] args) throws IOException, InterruptedException {
        String logFilePath = "live.log";

        // Step 1: Ensure a fresh start by deleting any existing "live.log" and creating an empty file
        File liveLogFile = new File(logFilePath);
        if (liveLogFile.exists()) {
            liveLogFile.delete();
        }
        liveLogFile.createNewFile();

        // Step 2: Initialize our parser, spike detector, and error grouper
        LogParser parser = new AppLogParser();
        SpikeDetector detector = new SpikeDetector(10, 3);
        ErrorGrouper grouper = new ErrorGrouper();

        // Step 3: Implement LogListener using an anonymous inner class to handle real-time callbacks
        LogListener listener = new LogListener() {
            @Override
            public void onEntry(LogEntry entry) {
                System.out.println(entry);
                // Group error patterns in real-time
                grouper.record(entry);
            }

            @Override
            public void onSpike(String service, int errorCount) {
                System.out.println("*** SPIKE ALERT: " + service + " has " + errorCount + " errors in the last 10 seconds ***");
            }

            @Override
            public void onMalformed(String reason, String rawLine) {
                System.out.println("SKIPPED (" + reason + "): " + rawLine);
            }
        };

        // Step 4: Create the watcher and launch it on its own background thread
        LogWatcher watcher = new LogWatcher(logFilePath, parser, detector, listener);
        Thread watcherThread = new Thread(watcher);
        watcherThread.start();

        // Step 5: Simulate live application traffic on the main thread using LogGenerator
        LogGenerator generator = new LogGenerator(logFilePath);

        // Phase A: Write 5 normal log lines, then pause
        generator.writeNormal(5);
        Thread.sleep(1000);

        // Phase B: Write 5 more normal log lines, then pause
        generator.writeNormal(5);
        Thread.sleep(1000);

        // Phase C: Simulate an outage with bursts of ERROR lines
        generator.writeBurst("payment-service", 20);
        generator.writeBurst("auth-service", 6);
        Thread.sleep(1000);

        // Phase D: Write 3 malformed lines to test error resilience
        generator.writeGarbage();
        Thread.sleep(1500);

        // Step 6: Signal the watcher to stop and wait for its thread to finish gracefully
        watcher.stop();
        watcherThread.join();

        // Step 7: Print the final summary report using the watcher's counters
        System.out.println();
        System.out.println("=== Live Watcher Summary ===");
        System.out.println("Total lines read: " + watcher.getLinesRead());
        System.out.println("Parsed OK:        " + watcher.getParsedOk());
        System.out.println("Malformed:        " + watcher.getMalformed());
        System.out.println("ERROR entries:    " + watcher.getErrorEntries());
        System.out.println("Spike alerts:     " + watcher.getSpikeAlerts());

        // Step 8: Print top error patterns grouped and sorted by frequency
        System.out.println();
        System.out.println("=== Top Error Patterns ===");
        for (Map.Entry<String, Integer> patternEntry : grouper.getTopPatterns()) {
            System.out.println(patternEntry.getValue() + " x " + patternEntry.getKey());
        }
    }
}
