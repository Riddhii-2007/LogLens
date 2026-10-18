package loglens.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import loglens.dao.IncidentDAO;
import loglens.engine.IncidentManager;
import loglens.model.Incident;
import loglens.util.IncidentExporter;
import loglens.util.SettingsStore;


// Dashboard panel for viewing, acknowledging, and resolving incidents.
//
// Why is the DAO used instead of writing SQL queries directly inside this panel?
// Adhering to the Separation of Concerns principle, UI components should focus purely on presentation,
// user gestures, and layout. All SQL syntax, schema details, and database connections remain cleanly
// isolated in the DAO layer.
public class IncidentPanel extends JPanel {

    private static final DateTimeFormatter FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final IncidentDAO dao;
    private final IncidentManager manager;

    private final JComboBox<String> statusFilterCombo;
    private final JTextField handledByField;
    private final JButton ackButton;
    private final JButton resolveButton;
    private final DefaultTableModel tableModel;
    private final JTable table;

    public IncidentPanel(final IncidentDAO dao, final IncidentManager manager) {
        super(new BorderLayout());
        this.dao = dao;
        this.manager = manager;

        // --- NORTH: Controls Panel ---
        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));

        northPanel.add(new JLabel("Status:"));
        String[] filterOptions = {"ALL", Incident.STATUS_OPEN, Incident.STATUS_ACKNOWLEDGED, Incident.STATUS_RESOLVED};
        statusFilterCombo = new JComboBox<String>(filterOptions);
        northPanel.add(statusFilterCombo);

        northPanel.add(new JLabel("Handled by:"));
        handledByField = new JTextField(12);
        northPanel.add(handledByField);

        ackButton = new JButton("Acknowledge");
        resolveButton = new JButton("Resolve");
        JButton refreshButton = new JButton("Refresh");
        JButton exportButton = new JButton("Export CSV");

        northPanel.add(ackButton);
        northPanel.add(resolveButton);
        northPanel.add(refreshButton);
        northPanel.add(exportButton);

        add(northPanel, BorderLayout.NORTH);

        // --- CENTER: Incidents Table ---
        String[] columnNames = {"ID", "Service", "Severity", "Status", "Pattern", "Opened", "Handled By"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(300);
        table.getColumnModel().getColumn(5).setPreferredWidth(140);
        table.getColumnModel().getColumn(6).setPreferredWidth(110);

        // Custom renderer for the Severity column
        table.getColumnModel().getColumn(2).setCellRenderer(new SeverityCellRenderer());

        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- Event Listeners ---

        // Filter change listener
        statusFilterCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });

        // Export CSV button listener
        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String selectedFilter = (String) statusFilterCombo.getSelectedItem();
                    List<Incident> incidents;
                    if ("ALL".equalsIgnoreCase(selectedFilter) || selectedFilter == null) {
                        incidents = dao.findAll();
                    } else {
                        incidents = dao.findByStatus(selectedFilter);
                    }

                    String exportFolder = SettingsStore.load().getExportFolder();
                    String path = IncidentExporter.export(incidents, exportFolder);

                    JOptionPane.showMessageDialog(IncidentPanel.this, 
                            "Incidents exported successfully to:\n" + path, 
                            "Export Successful", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(IncidentPanel.this, 
                            "Error exporting incidents to CSV: " + ex.getMessage(), 
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(IncidentPanel.this, 
                            "Database error loading incidents for export: " + ex.getMessage(), 
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });


        // Refresh button listener
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });

        // Acknowledge button listener
        ackButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow < 0) {
                    JOptionPane.showMessageDialog(IncidentPanel.this, 
                            "Please select an incident to acknowledge.", 
                            "No Selection", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String handler = handledByField.getText().trim();
                if (handler.isEmpty()) {
                    JOptionPane.showMessageDialog(IncidentPanel.this, 
                            "Please enter a non-empty name in 'Handled by'.", 
                            "Missing Handler", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int incidentId = (Integer) table.getValueAt(selectedRow, 0);
                try {
                    boolean success = dao.acknowledge(incidentId, handler);
                    if (success) {
                        refresh();
                    } else {
                        JOptionPane.showMessageDialog(IncidentPanel.this, 
                                "Could not acknowledge incident #" + incidentId + ". Only OPEN incidents can be acknowledged.", 
                                "Action Failed", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(IncidentPanel.this, 
                            "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Resolve button listener
        resolveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow < 0) {
                    JOptionPane.showMessageDialog(IncidentPanel.this, 
                            "Please select an incident to resolve.", 
                            "No Selection", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int incidentId = (Integer) table.getValueAt(selectedRow, 0);
                String service = (String) table.getValueAt(selectedRow, 1);

                try {
                    boolean success = dao.resolve(incidentId);
                    if (success) {
                        manager.incidentClosed(service);
                        refresh();
                    } else {
                        JOptionPane.showMessageDialog(IncidentPanel.this, 
                                "Only acknowledged incidents can be resolved.", 
                                "Action Failed", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(IncidentPanel.this, 
                            "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Double-click row listener to show full details
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow >= 0) {
                        int incidentId = (Integer) table.getValueAt(selectedRow, 0);
                        try {
                            Incident incident = dao.findById(incidentId);
                            if (incident != null) {
                                String details = incident.toString() + "\n\n"
                                        + "Opened At:       " + incident.getOpenedAt() + "\n"
                                        + "Acknowledged At: " + incident.getAcknowledgedAt() + "\n"
                                        + "Resolved At:     " + incident.getResolvedAt() + "\n"
                                        + "Handled By:      " + incident.getHandledBy();

                                JOptionPane.showMessageDialog(IncidentPanel.this, details, 
                                        "Incident #" + incidentId + " Details", JOptionPane.INFORMATION_MESSAGE);
                            }
                        } catch (SQLException ex) {
                            JOptionPane.showMessageDialog(IncidentPanel.this, 
                                    "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        // Periodic background polling timer (every 2000 ms)
        Timer timer = new Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
        timer.start();

        // Initial load
        refresh();
    }

    // Loads incidents from the DAO according to the selected filter and preserves selection
    public void refresh() {
        int previouslySelectedId = -1;
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < table.getRowCount()) {
            previouslySelectedId = (Integer) table.getValueAt(selectedRow, 0);
        }

        try {
            String selectedFilter = (String) statusFilterCombo.getSelectedItem();
            List<Incident> incidents;

            if ("ALL".equalsIgnoreCase(selectedFilter) || selectedFilter == null) {
                incidents = dao.findAll();
            } else {
                incidents = dao.findByStatus(selectedFilter);
            }

            tableModel.setRowCount(0);

            int restoredRowIndex = -1;
            for (int i = 0; i < incidents.size(); i++) {
                Incident inc = incidents.get(i);
                String openedStr = (inc.getOpenedAt() != null) ? inc.getOpenedAt().format(FORMATTER) : "N/A";

                Object[] rowData = {
                    inc.getId(),
                    inc.getService(),
                    inc.getSeverity(),
                    inc.getStatus(),
                    inc.getPattern(),
                    openedStr,
                    (inc.getHandledBy() != null ? inc.getHandledBy() : "")
                };

                tableModel.addRow(rowData);

                if (inc.getId() == previouslySelectedId) {
                    restoredRowIndex = i;
                }
            }

            // Restore selection if the incident is still present in the list
            if (restoredRowIndex >= 0) {
                table.setRowSelectionInterval(restoredRowIndex, restoredRowIndex);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                    "Failed to refresh incidents: " + e.getMessage(), 
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Configures read-only mode, enabling or disabling acknowledge/resolve buttons and the handled-by field
    public void setReadOnly(boolean readOnly) {
        ackButton.setEnabled(!readOnly);
        resolveButton.setEnabled(!readOnly);
        handledByField.setEnabled(!readOnly);
    }

    // Custom renderer that highlights the Severity column
    private static class SeverityCellRenderer extends DefaultTableCellRenderer {
        private static final Color COLOR_CRITICAL = new Color(255, 180, 180); // Light red
        private static final Color COLOR_HIGH = new Color(255, 215, 160);     // Light orange
        private static final Color COLOR_MEDIUM = new Color(255, 250, 180);   // Light yellow

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
            } else {
                c.setForeground(Color.BLACK);
                String severity = (value != null) ? value.toString() : "";

                if ("CRITICAL".equalsIgnoreCase(severity)) {
                    c.setBackground(COLOR_CRITICAL);
                } else if ("HIGH".equalsIgnoreCase(severity)) {
                    c.setBackground(COLOR_HIGH);
                } else if ("MEDIUM".equalsIgnoreCase(severity)) {
                    c.setBackground(COLOR_MEDIUM);
                } else {
                    c.setBackground(Color.WHITE);
                }
            }

            return c;
        }
    }
}
