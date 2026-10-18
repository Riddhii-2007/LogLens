package loglens.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import loglens.model.Incident;

// Utility class for exporting incident records to formatted CSV files.
public class IncidentExporter {

    private static final DateTimeFormatter FILE_NAME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private IncidentExporter() {
    }

    // Exports a list of Incident objects into a timestamped CSV file inside the specified folder.
    // Returns the file path of the generated CSV file.
    public static String export(List<Incident> incidents, String folder) throws IOException {
        if (folder == null || folder.trim().isEmpty()) {
            folder = "exports";
        }

        File directory = new File(folder);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = "incidents_" + LocalDateTime.now().format(FILE_NAME_FORMATTER) + ".csv";
        File targetFile = new File(directory, fileName);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
            // Write CSV header row
            StringBuffer header = new StringBuffer();
            header.append(escapeCsv("ID")).append(",")
                  .append(escapeCsv("Service")).append(",")
                  .append(escapeCsv("Severity")).append(",")
                  .append(escapeCsv("Status")).append(",")
                  .append(escapeCsv("Pattern")).append(",")
                  .append(escapeCsv("Opened At")).append(",")
                  .append(escapeCsv("Acknowledged At")).append(",")
                  .append(escapeCsv("Resolved At")).append(",")
                  .append(escapeCsv("Handled By"));
            writer.write(header.toString());
            writer.newLine();

            // Write data rows
            if (incidents != null) {
                for (Incident inc : incidents) {
                    StringBuffer row = new StringBuffer();
                    String openedStr = (inc.getOpenedAt() != null) ? inc.getOpenedAt().format(DATE_FORMATTER) : "";
                    String ackStr = (inc.getAcknowledgedAt() != null) ? inc.getAcknowledgedAt().format(DATE_FORMATTER) : "";
                    String resStr = (inc.getResolvedAt() != null) ? inc.getResolvedAt().format(DATE_FORMATTER) : "";

                    row.append(inc.getId()).append(",")
                       .append(escapeCsv(inc.getService())).append(",")
                       .append(escapeCsv(inc.getSeverity())).append(",")
                       .append(escapeCsv(inc.getStatus())).append(",")
                       .append(escapeCsv(inc.getPattern())).append(",")
                       .append(escapeCsv(openedStr)).append(",")
                       .append(escapeCsv(ackStr)).append(",")
                       .append(escapeCsv(resStr)).append(",")
                       .append(escapeCsv(inc.getHandledBy() != null ? inc.getHandledBy() : ""));

                    writer.write(row.toString());
                    writer.newLine();
                }
            }
        }

        return targetFile.getAbsolutePath();
    }

    // Helper method to wrap values in double quotes and escape internal quotes by doubling them
    private static String escapeCsv(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
