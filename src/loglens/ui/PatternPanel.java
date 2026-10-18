package loglens.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

import loglens.engine.ErrorGrouper;

// Displays top recurring error patterns grouped and sorted by frequency in real-time.
public class PatternPanel extends JPanel {

    private final ErrorGrouper grouper;
    private final DefaultTableModel tableModel;
    private final JTable table;

    public PatternPanel(final ErrorGrouper grouper) {
        super(new BorderLayout());
        this.grouper = grouper;

        String[] columns = {"Count", "Pattern"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(750);

        add(new JScrollPane(table), BorderLayout.CENTER);

        // Timer that polls the ErrorGrouper every 1000 ms to refresh patterns
        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });
        timer.start();

        refresh();
    }

    // Fetches top patterns from ErrorGrouper and rebuilds table rows
    public void refresh() {
        List<Map.Entry<String, Integer>> patterns = grouper.getTopPatterns();
        tableModel.setRowCount(0);

        for (Map.Entry<String, Integer> entry : patterns) {
            Object[] rowData = {entry.getValue(), entry.getKey()};
            tableModel.addRow(rowData);
        }
    }
}
