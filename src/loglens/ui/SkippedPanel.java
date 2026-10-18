package loglens.ui;

import java.awt.BorderLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

// Displays unparseable, malformed, or corrupt log lines rejected by LogParser.
//
// Why do skipped lines get their own dedicated tab?
// In production logging, malformed log lines are often early warning indicators of
// broken logging configurations, network corruption, or unhandled exceptions in microservices.
// Silently dropping them hides these failures, while mixing them directly into the clean log table
// breaks parsing and filtering. Giving skipped lines their own tab provides full visibility and auditing
// without polluting valid log workflows.
public class SkippedPanel extends JPanel {

    private static final int MAX_ROWS = 500;
    private static final DateTimeFormatter FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DefaultTableModel tableModel;
    private final JTable table;

    // Cumulative counter tracking all skipped lines received since application launch
    private int totalSkippedCount = 0;

    public SkippedPanel() {
        super(new BorderLayout());

        String[] columns = {"Time", "Reason", "Raw Line"};

        // Non-editable table model
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(140);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(550);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    // Appends a skipped line entry; thread-safe so it can be called from any background thread
    public void addSkipped(final String reason, final String rawLine) {
        if (SwingUtilities.isEventDispatchThread()) {
            doAddSkipped(reason, rawLine);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    doAddSkipped(reason, rawLine);
                }
            });
        }
    }

    // Internal helper executed strictly on the Event Dispatch Thread (EDT)
    private void doAddSkipped(String reason, String rawLine) {
        totalSkippedCount++;

        String timeStr = LocalDateTime.now().format(FORMATTER);
        Object[] rowData = {timeStr, reason, rawLine};
        tableModel.addRow(rowData);

        // Maintain maximum 500 rows in memory
        if (tableModel.getRowCount() > MAX_ROWS) {
            tableModel.removeRow(0);
        }

        // Scroll viewport to newest skipped line
        int lastRow = tableModel.getRowCount() - 1;
        if (lastRow >= 0) {
            table.scrollRectToVisible(table.getCellRect(lastRow, 0, true));
        }
    }

    // Returns the total number of skipped lines processed since startup
    public int getCount() {
        return totalSkippedCount;
    }
}
