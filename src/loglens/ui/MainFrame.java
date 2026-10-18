package loglens.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import loglens.dao.IncidentDAO;
import loglens.engine.ErrorGrouper;
import loglens.engine.IncidentManager;
import loglens.model.Settings;
import loglens.model.User;
import loglens.util.LogGenerator;
import loglens.util.SettingsStore;

// Main application window for the LogLens dashboard.
//
// What is CardLayout?
// CardLayout is a Swing layout manager that treats each added component as a "card" in a deck.
// Only one card is visible at any given time while the others remain hidden in memory.
// Calling cardLayout.show(container, "CARD_NAME") lets us swap views instantly (e.g. from the
// "LOGIN" screen to the "DASHBOARD" screen and back on logout) within the exact same window frame.
public class MainFrame extends JFrame {

    private final CardLayout cardLayout;
    private final LoginPanel loginPanel;
    private final JLabel statusLabel;
    private final LiveLogPanel liveLogPanel;
    private final IncidentPanel incidentPanel;
    private final PatternPanel patternPanel;
    private final SkippedPanel skippedPanel;
    private final JTabbedPane tabbedPane;
    private final JButton logoutBtn;
    private Runnable onCloseAction;

    // Flag indicating whether an alert is pinned to the status line
    private boolean alertPinned = false;

    // Primary constructor with full panel injection including LoginPanel
    public MainFrame(LiveLogPanel liveLogPanel, IncidentPanel incidentPanel, 
                     PatternPanel patternPanel, SkippedPanel skippedPanel, LoginPanel loginPanel) {
        super("LogLens - Log Monitoring & Incident Dashboard");
        this.liveLogPanel = liveLogPanel;
        this.incidentPanel = incidentPanel;
        this.patternPanel = patternPanel;
        this.skippedPanel = skippedPanel;
        this.loginPanel = (loginPanel != null) ? loginPanel : new LoginPanel();

        setSize(1000, 650);
        setLocationRelativeTo(null); // Centered on screen
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Frame content area uses CardLayout to toggle between LOGIN and DASHBOARD cards
        cardLayout = new CardLayout();
        getContentPane().setLayout(cardLayout);

        // Card 1: LOGIN card
        getContentPane().add(this.loginPanel, "LOGIN");

        // Card 2: DASHBOARD card (encloses status bar, tabbed panels, and control toolbar)
        JPanel dashboardPanel = new JPanel(new BorderLayout());

        // --- NORTH: Status Line ---
        statusLabel = new JLabel(" Ready - Monitoring live.log");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(240, 240, 240));
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        dashboardPanel.add(statusLabel, BorderLayout.NORTH);

        // --- CENTER: Tabs ---
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Live Logs", this.liveLogPanel);
        tabbedPane.addTab("Incidents", this.incidentPanel);
        tabbedPane.addTab("Patterns", this.patternPanel);
        tabbedPane.addTab("Skipped Lines", this.skippedPanel);
        dashboardPanel.add(tabbedPane, BorderLayout.CENTER);

        // --- SOUTH: Mock Traffic Generator, Settings & Logout Panel ---
        // Why do simulator buttons spawn their own threads?
        // Appending records to a file on disk involves disk I/O. If executed directly on the
        // Swing Event Dispatch Thread (EDT), the entire UI would freeze and become unresponsive
        // while the file is being written. Running the generation task on a background thread keeps
        // the user interface smooth and fluid.
        JPanel southPanel = new JPanel(new GridLayout(1, 6, 8, 8));
        southPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton normalTrafficBtn = new JButton("Normal traffic (20 lines)");
        JButton burstPaymentBtn = new JButton("Error burst: payment-service (12)");
        JButton burstAuthBtn = new JButton("Error burst: auth-service (6)");
        JButton garbageBtn = new JButton("Garbage lines");
        JButton settingsBtn = new JButton("Settings");
        logoutBtn = new JButton("Logout");

        normalTrafficBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            new LogGenerator("live.log").writeNormal(20);
                            setStatus("Generated 20 normal log lines");
                        } catch (IOException ex) {
                            setStatus("Error generating normal traffic: " + ex.getMessage());
                        }
                    }
                }).start();
            }
        });

        burstPaymentBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            new LogGenerator("live.log").writeBurst("payment-service", 12);
                            setAlert("Generated 12 error burst lines for payment-service");
                        } catch (IOException ex) {
                            setStatus("Error generating burst: " + ex.getMessage());
                        }
                    }
                }).start();
            }
        });

        burstAuthBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            new LogGenerator("live.log").writeBurst("auth-service", 6);
                            setAlert("Generated 6 error burst lines for auth-service");
                        } catch (IOException ex) {
                            setStatus("Error generating burst: " + ex.getMessage());
                        }
                    }
                }).start();
            }
        });

        garbageBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            new LogGenerator("live.log").writeGarbage();
                            setStatus("Generated 3 malformed/garbage log lines");
                        } catch (IOException ex) {
                            setStatus("Error generating garbage: " + ex.getMessage());
                        }
                    }
                }).start();
            }
        });

        // Settings dialog button
        settingsBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Settings currentSettings = SettingsStore.load();

                JSpinner windowSecondsSpinner = new JSpinner(
                        new SpinnerNumberModel(currentSettings.getWindowSeconds(), 1, 3600, 1));
                JSpinner thresholdSpinner = new JSpinner(
                        new SpinnerNumberModel(currentSettings.getThreshold(), 1, 1000, 1));
                JTextField exportFolderField = new JTextField(currentSettings.getExportFolder(), 15);

                JPanel dialogPanel = new JPanel(new GridLayout(3, 2, 8, 8));
                dialogPanel.add(new JLabel("Window Seconds:"));
                dialogPanel.add(windowSecondsSpinner);
                dialogPanel.add(new JLabel("Threshold:"));
                dialogPanel.add(thresholdSpinner);
                dialogPanel.add(new JLabel("Export Folder:"));
                dialogPanel.add(exportFolderField);

                int result = JOptionPane.showConfirmDialog(MainFrame.this, dialogPanel, 
                        "LogLens Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (result == JOptionPane.OK_OPTION) {
                    currentSettings.setWindowSeconds(((Number) windowSecondsSpinner.getValue()).intValue());
                    currentSettings.setThreshold(((Number) thresholdSpinner.getValue()).intValue());
                    currentSettings.setExportFolder(exportFolderField.getText().trim());

                    SettingsStore.save(currentSettings);
                    setStatus("Settings saved successfully.");
                }
            }
        });

        // Logout button returns to LOGIN card and clears input fields
        logoutBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(getContentPane(), "LOGIN");
                MainFrame.this.loginPanel.clearFields();
                alertPinned = false;
                statusLabel.setForeground(Color.BLACK);
                statusLabel.setText(" Logged out");
            }
        });

        southPanel.add(normalTrafficBtn);
        southPanel.add(burstPaymentBtn);
        southPanel.add(burstAuthBtn);
        southPanel.add(garbageBtn);
        southPanel.add(settingsBtn);
        southPanel.add(logoutBtn);

        dashboardPanel.add(southPanel, BorderLayout.SOUTH);

        // Add DASHBOARD card
        getContentPane().add(dashboardPanel, "DASHBOARD");

        // Application starts on LOGIN card
        cardLayout.show(getContentPane(), "LOGIN");

        // Window listener to trigger graceful shutdown actions
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (onCloseAction != null) {
                    onCloseAction.run();
                }
            }
        });
    }

    // Overloaded constructor when panels are created externally without LoginPanel
    public MainFrame(LiveLogPanel liveLogPanel, IncidentPanel incidentPanel, 
                     PatternPanel patternPanel, SkippedPanel skippedPanel) {
        this(liveLogPanel, incidentPanel, patternPanel, skippedPanel, new LoginPanel());
    }

    // Overloaded constructor when panels are created externally without SkippedPanel and LoginPanel
    public MainFrame(LiveLogPanel liveLogPanel, IncidentPanel incidentPanel, PatternPanel patternPanel) {
        this(liveLogPanel, incidentPanel, patternPanel, new SkippedPanel(), new LoginPanel());
    }

    // Convenience constructor that instantiates child panels automatically
    public MainFrame(IncidentDAO dao, IncidentManager manager, ErrorGrouper grouper) {
        this(new LiveLogPanel(), new IncidentPanel(dao, manager), new PatternPanel(grouper), 
             new SkippedPanel(), new LoginPanel());
    }

    // Switches the card to DASHBOARD, sets status text, and applies role permissions (read-only for VIEWER)
    public void showDashboard(final User user) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (user != null) {
                    boolean isViewer = User.ROLE_VIEWER.equalsIgnoreCase(user.getRole());
                    incidentPanel.setReadOnly(isViewer);
                    alertPinned = false;
                    statusLabel.setForeground(Color.BLACK);
                    statusLabel.setText(" Logged in as " + user.getUsername() + " (" + user.getRole() + ")");
                }
                cardLayout.show(getContentPane(), "DASHBOARD");
            }
        });
    }

    // Displays an alert in red text and pins it so that subsequent setStatus calls (e.g., from malformed lines)
    // do not overwrite it. A new call to setAlert replaces the old one.
    public void setAlert(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                alertPinned = true;
                statusLabel.setForeground(Color.RED);
                statusLabel.setText(" [ALERT] " + text);
            }
        });
    }

    // Thread-safe method to update status bar text.
    // If an alert is currently pinned, regular status updates will not overwrite it.
    public void setStatus(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (alertPinned) {
                    return;
                }
                statusLabel.setForeground(Color.BLACK);
                statusLabel.setText(" " + text);
            }
        });
    }

    // Forwards a skipped/malformed log line to the SkippedPanel and updates the tab title with the count
    public void addSkipped(final String reason, final String rawLine) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                skippedPanel.addSkipped(reason, rawLine);
                int index = tabbedPane.indexOfComponent(skippedPanel);
                if (index >= 0) {
                    tabbedPane.setTitleAt(index, "Skipped Lines (" + skippedPanel.getCount() + ")");
                }
            }
        });
    }

    // Getter for the LiveLogPanel
    public LiveLogPanel getLiveLogPanel() {
        return liveLogPanel;
    }

    // Getter for the SkippedPanel
    public SkippedPanel getSkippedPanel() {
        return skippedPanel;
    }

    // Getter for the LoginPanel
    public LoginPanel getLoginPanel() {
        return loginPanel;
    }

    // Configures a cleanup callback when window closes
    public void setOnClose(Runnable action) {
        this.onCloseAction = action;
    }
}
