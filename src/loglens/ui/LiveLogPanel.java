package loglens.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import loglens.model.LogEntry;

// Displays a real-time scrolling view of log events as they are processed, with dynamic search filtering.
public class LiveLogPanel extends JPanel {

    private static final int MAX_ROWS = 500;
    private static final DateTimeFormatter FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;

    public LiveLogPanel() {
        super(new BorderLayout());

        // --- NORTH: Search Filter Panel ---
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        searchPanel.add(new JLabel("Search:"));
        final JTextField searchField = new JTextField(25);
        searchPanel.add(searchField);
        add(searchPanel, BorderLayout.NORTH);

        // Define column headers
        String[] columns = {"Time", "Level", "Service", "Message"};

        // Non-editable table model
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);

        // TableRowSorter for search filtering across any column
        sorter = new TableRowSorter<DefaultTableModel>(tableModel);
        table.setRowSorter(sorter);

        // DocumentListener to dynamically filter rows as the user types
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFilter();
            }

            private void updateFilter() {
                String text = searchField.getText().trim();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    // Case-insensitive regex matching any column
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
                }
            }
        });

        // Apply custom row renderer for color coding
        LogLevelCellRenderer renderer = new LogLevelCellRenderer();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // Set preferred column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(70);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(500);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    // Why must UI updates use SwingUtilities.invokeLater?
    // Swing is single-threaded and not thread-safe. All UI creation and visual modifications
    // must happen exclusively on the Event Dispatch Thread (EDT). Since addEntry is called
    // by the background LogWatcher thread, wrapping table modifications in invokeLater
    // schedules the task onto the EDT, preventing graphical glitches and thread deadlocks.
    public void addEntry(final LogEntry entry) {
        if (entry == null) {
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String timeStr = (entry.getTimestamp() != null) ? entry.getTimestamp().format(FORMATTER) : "N/A";
                Object[] rowData = {timeStr, entry.getLevel(), entry.getService(), entry.getMessage()};

                tableModel.addRow(rowData);

                // Cap maximum rows in memory to prevent memory bloat
                if (tableModel.getRowCount() > MAX_ROWS) {
                    tableModel.removeRow(0);
                }

                // Automatically scroll viewport to newest row using convertRowIndexToView
                int lastModelRow = tableModel.getRowCount() - 1;
                if (lastModelRow >= 0) {
                    int lastViewRow = table.convertRowIndexToView(lastModelRow);
                    if (lastViewRow >= 0) {
                        table.scrollRectToVisible(table.getCellRect(lastViewRow, 0, true));
                    }
                }
            }
        });
    }

    // How does a TableCellRenderer work?
    // JTable does not create a separate JLabel widget for every cell. Instead, it reuses a single
    // renderer component like a rubber stamp: for each cell to draw, it passes the cell's value and
    // state to getTableCellRendererComponent, configures its colors/text, and paints it onto the screen.
    private static class LogLevelCellRenderer extends DefaultTableCellRenderer {
        private static final Color COLOR_ERROR = new Color(255, 230, 230); // Soft red
        private static final Color COLOR_WARN = new Color(255, 245, 220);  // Soft orange/yellow

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // Respect selection highlighting
            if (isSelected) {
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
            } else {
                c.setForeground(Color.BLACK);
                int modelRow = table.convertRowIndexToModel(row);
                Object levelObj = table.getModel().getValueAt(modelRow, 1);
                String level = (levelObj != null) ? levelObj.toString() : "";

                if ("ERROR".equalsIgnoreCase(level)) {
                    c.setBackground(COLOR_ERROR);
                } else if ("WARN".equalsIgnoreCase(level)) {
                    c.setBackground(COLOR_WARN);
                } else {
                    c.setBackground(Color.WHITE);
                }
            }

            return c;
        }
    }
}
