package loglens.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

// Utility class that generates realistic mock log files for testing LogLens.
// It can simulate normal day-to-day traffic, sudden error spikes (bursts), and corrupt lines (garbage).
public class LogGenerator {

    // Target file path where log entries will be appended
    private final String filePath;

    // Formatter used to display the current date and time as "yyyy-MM-dd HH:mm:ss"
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Random number generator used to pick services, levels, and IDs
    private final Random random = new Random();

    // List of simulated microservices in our system
    private static final String[] SERVICES = {
        "payment-service", 
        "auth-service", 
        "order-service"
    };

    // Constructor: receives the target file path and stores it
    public LogGenerator(String filePath) {
        this.filePath = filePath;
    }

    // Appends 'count' normal log lines with a realistic distribution:
    // ~80% INFO, ~15% WARN, ~5% ERROR.
    public void writeNormal(int count) throws IOException {
        // 'true' in FileWriter enables APPEND mode so existing lines are preserved
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            for (int i = 0; i < count; i++) {
                String service = getRandomService();
                int roll = random.nextInt(100);
                String level;
                String message;

                if (roll < 80) {
                    level = "INFO";
                    message = getRandomInfoMessage(service);
                } else if (roll < 95) {
                    level = "WARN";
                    message = getRandomWarnMessage(service);
                } else {
                    level = "ERROR";
                    message = getRandomErrorMessage();
                }

                writer.write(formatLine(level, service, message));
                writer.newLine();
            }
        }
    }

    // Appends 'count' consecutive ERROR lines for a single service to simulate an outage or burst
    public void writeBurst(String service, int count) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            for (int i = 0; i < count; i++) {
                String message = getRandomErrorMessage();
                writer.write(formatLine("ERROR", service, message));
                writer.newLine();
            }
        }
    }

    // Appends 3 deliberately broken lines to test the parser's error handling:
    // 1. Missing fields (less than 5 tokens)
    // 2. Invalid log level (e.g., DEBUG or lowercase)
    // 3. Impossible timestamp (e.g., 99:99:99)
    public void writeGarbage() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            // Line 1: Missing tokens
            writer.write("this line is broken");
            writer.newLine();

            // Line 2: Invalid level (LogParser only accepts INFO, WARN, ERROR)
            writer.write(LocalDateTime.now().format(DATE_TIME_FORMATTER) + " DEBUG auth-service Unknown level");
            writer.newLine();

            // Line 3: Impossible date/time
            writer.write("2026-10-18 99:99:99 ERROR auth-service Bad time");
            writer.newLine();
        }
    }

    // Helper method to format a valid line using the current date and time
    private String formatLine(String level, String service, String message) {
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        return timestamp + " " + level + " " + service + " " + message;
    }

    // Helper method to randomly select one of the known services
    private String getRandomService() {
        return SERVICES[random.nextInt(SERVICES.length)];
    }

    // Helper method to produce realistic INFO messages
    private String getRandomInfoMessage(String service) {
        if ("auth-service".equals(service)) {
            return "User session started";
        } else if ("order-service".equals(service)) {
            return "Request completed in " + (100 + random.nextInt(150)) + " ms";
        } else {
            return "Transaction processed successfully with ID " + (10000 + random.nextInt(90000));
        }
    }

    // Helper method to produce realistic WARN messages
    private String getRandomWarnMessage(String service) {
        if ("order-service".equals(service)) {
            return "Slow query detected";
        } else if ("auth-service".equals(service)) {
            return "Multiple failed login attempts detected";
        } else {
            return "High latency observed in payment gateway";
        }
    }

    // Helper method to produce realistic ERROR messages with varying values
    private String getRandomErrorMessage() {
        if (random.nextBoolean()) {
            int ipSuffix = 1 + random.nextInt(9);
            return "Timeout connecting to DB at 10.0.0." + ipSuffix;
        } else {
            int orderNumber = 1000 + random.nextInt(9000);
            return "Payment gateway returned 503 for order " + orderNumber;
        }
    }
}
