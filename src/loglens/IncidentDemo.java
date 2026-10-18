package loglens;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import loglens.dao.DBConnection;
import loglens.dao.IncidentDAO;
import loglens.engine.ErrorGrouper;
import loglens.engine.IncidentManager;
import loglens.engine.LogListener;
import loglens.engine.LogWatcher;
import loglens.engine.SpikeDetector;
import loglens.model.Incident;
import loglens.model.LogEntry;
import loglens.parser.AppLogParser;
import loglens.parser.LogParser;
import loglens.util.LogGenerator;

// Demonstrates automated incident creation driven by live log analysis and spike detection.
public class IncidentDemo {

    public static void main(String[] args) throws IOException, InterruptedException, SQLException {
        String logFilePath = "live.log";
        String dbFilePath = "loglens.db";

        // Step 1: Delete existing files to start clean
        File liveLogFile = new File(logFilePath);
        if (liveLogFile.exists()) {
            liveLogFile.delete();
        }

        File dbFile = new File(dbFilePath);
        if (dbFile.exists()) {
            dbFile.delete();
        }

        // Step 2: Initialize SQLite database schema
        DBConnection.createTables();

        // Step 3: Initialize engine components
        LogParser parser = new AppLogParser();
        SpikeDetector detector = new SpikeDetector(10, 3);
        ErrorGrouper grouper = new ErrorGrouper();
        IncidentDAO dao = new IncidentDAO();

        // Downstream listener that only prints spike alerts and malformed lines (not every normal entry)
        LogListener downstreamListener = new LogListener() {
            @Override
            public void onEntry(LogEntry entry) {
                // Intentionally silent on normal entries
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

        // IncidentManager decorates the listener pipeline to create database incidents on spikes
        IncidentManager manager = new IncidentManager(dao, grouper, detector, downstreamListener);


        // Step 4: Create and launch the background watcher thread
        LogWatcher watcher = new LogWatcher(logFilePath, parser, detector, manager);
        Thread watcherThread = new Thread(watcher);
        watcherThread.start();

        // Step 5: Simulate traffic with LogGenerator
        LogGenerator generator = new LogGenerator(logFilePath);

        generator.writeNormal(10);
        Thread.sleep(1000);

        generator.writeBurst("payment-service", 12);
        Thread.sleep(1000);

        generator.writeBurst("auth-service", 4);
        Thread.sleep(1000);

        generator.writeGarbage();
        Thread.sleep(1500);

        // Step 6: Stop the watcher and wait for it to finish
        watcher.stop();
        watcherThread.join();

        // Step 7: Print processing summary
        System.out.println();
        System.out.println("=== Live Watcher Summary ===");
        System.out.println("Total lines read: " + watcher.getLinesRead());
        System.out.println("Parsed OK:        " + watcher.getParsedOk());
        System.out.println("Malformed:        " + watcher.getMalformed());
        System.out.println("ERROR entries:    " + watcher.getErrorEntries());
        System.out.println("Spike alerts:     " + watcher.getSpikeAlerts());

        // Step 8: Print all recorded incidents from the database
        System.out.println();
        System.out.println("=== Incidents in Database ===");
        for (Incident inc : dao.findAll()) {
            System.out.println("  " + inc);
        }
    }
}
