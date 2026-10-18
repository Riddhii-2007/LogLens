package loglens.ui;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import loglens.dao.DBConnection;
import loglens.dao.IncidentDAO;
import loglens.dao.UserDAO;
import loglens.engine.ErrorGrouper;
import loglens.engine.IncidentManager;
import loglens.engine.LogListener;
import loglens.engine.LogWatcher;
import loglens.engine.SpikeDetector;
import loglens.model.LogEntry;
import loglens.model.Settings;
import loglens.model.User;
import loglens.parser.AppLogParser;
import loglens.parser.LogParser;
import loglens.util.SettingsStore;

// Application bootstrap and entry point for the LogLens GUI dashboard.
public class AppLauncher {

    public static void main(String[] args) throws IOException {
        // Set native system look and feel for a modern platform appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Default look and feel used as fallback
        }

        // Initialize clean live.log file
        File liveLog = new File("live.log");
        if (liveLog.exists()) {
            liveLog.delete();
        }
        liveLog.createNewFile();

        // Initialize SQLite database schema (loglens.db is preserved between runs so incidents persist)
        DBConnection.createTables();

        // Seed default users (admin / admin123, viewer / viewer123) if users table is empty
        try {
            new UserDAO().seedDefaultUsers();
        } catch (SQLException e) {
            System.err.println("Error seeding default users: " + e.getMessage());
        }

        // Load persisted settings (window seconds, threshold, export directory)
        final Settings settings = SettingsStore.load();

        // Instantiate core engine models and services configured with loaded settings
        final LogParser parser = new AppLogParser();
        final SpikeDetector detector = new SpikeDetector(settings.getWindowSeconds(), settings.getThreshold());
        final ErrorGrouper grouper = new ErrorGrouper();
        final IncidentDAO dao = new IncidentDAO();

        // Launch UI strictly on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(new Runnable() {
            final MainFrame[] mainFrameHolder = new MainFrame[1];

            @Override
            public void run() {
                final LiveLogPanel liveLogPanel = new LiveLogPanel();
                final PatternPanel patternPanel = new PatternPanel(grouper);
                final SkippedPanel skippedPanel = new SkippedPanel();

                // Create LoginPanel with callback listener switching to DASHBOARD card upon successful login
                final LoginPanel loginPanel = new LoginPanel(new LoginPanel.LoginListener() {
                    @Override
                    public void onLoginSuccess(User user) {
                        if (mainFrameHolder[0] != null) {
                            mainFrameHolder[0].showDashboard(user);
                        }
                    }
                });

                // Downstream listener forwarding events to UI components
                LogListener uiListener = new LogListener() {
                    @Override
                    public void onEntry(LogEntry entry) {
                        liveLogPanel.addEntry(entry);
                    }

                    // Spike events use setAlert to display prominent red pinned alerts
                    // that cannot be overwritten by subsequent malformed log warnings.
                    @Override
                    public void onSpike(String service, int errorCount) {
                        if (mainFrameHolder[0] != null) {
                            String alertText = "SPIKE ALERT: " + service + " (" + errorCount 
                                    + " errors in the last " + settings.getWindowSeconds() + " seconds)";
                            mainFrameHolder[0].setAlert(alertText);
                        }
                    }

                    // Malformed lines are recorded in the dedicated Skipped Lines tab and update the status bar
                    @Override
                    public void onMalformed(String reason, String rawLine) {
                        if (mainFrameHolder[0] != null) {
                            mainFrameHolder[0].addSkipped(reason, rawLine);
                            mainFrameHolder[0].setStatus("SKIPPED (" + reason + "): " + rawLine);
                        }
                    }
                };

                // Pipeline: Watcher -> IncidentManager -> uiListener -> Swing Components
                final IncidentManager manager = new IncidentManager(dao, grouper, detector, uiListener);
                final IncidentPanel incidentPanel = new IncidentPanel(dao, manager);

                final MainFrame mainFrame = new MainFrame(liveLogPanel, incidentPanel, patternPanel, skippedPanel, loginPanel);
                mainFrameHolder[0] = mainFrame;

                // Start background LogWatcher thread
                final LogWatcher watcher = new LogWatcher("live.log", parser, detector, manager);
                Thread watcherThread = new Thread(watcher, "LogWatcher-Thread");
                watcherThread.start();

                // Stop watcher when the user closes the window
                mainFrame.setOnClose(new Runnable() {
                    @Override
                    public void run() {
                        watcher.stop();
                    }
                });

                mainFrame.setVisible(true);
            }
        });
    }
}
