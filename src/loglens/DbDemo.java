package loglens;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import loglens.dao.DBConnection;
import loglens.dao.IncidentDAO;
import loglens.model.Incident;

// Console demonstration of SQLite database operations using the IncidentDAO.
public class DbDemo {

    public static void main(String[] args) {
        // Step 1: Delete existing database file to ensure a clean run
        File dbFile = new File("loglens.db");
        if (dbFile.exists()) {
            dbFile.delete();
        }

        try {
            // Step 2: Initialize SQLite database tables
            System.out.println("--- Initializing Database ---");
            DBConnection.createTables();

            IncidentDAO dao = new IncidentDAO();

            // Step 3: Insert 3 incidents
            System.out.println("\n--- Inserting Incidents ---");
            Incident inc1 = new Incident("payment-service", "Timeout connecting to DB at <IP>", "HIGH", LocalDateTime.now());
            Incident inc2 = new Incident("auth-service", "Payment gateway returned <N> for order <N>", "CRITICAL", LocalDateTime.now());
            Incident inc3 = new Incident("order-service", "Slow query detected", "LOW", LocalDateTime.now());

            int id1 = dao.insert(inc1);
            int id2 = dao.insert(inc2);
            int id3 = dao.insert(inc3);

            System.out.println("Inserted Incident 1 with ID: " + id1);
            System.out.println("Inserted Incident 2 with ID: " + id2);
            System.out.println("Inserted Incident 3 with ID: " + id3);

            // Step 4: Print all incidents
            System.out.println("\n--- All Incidents ---");
            printIncidents(dao.findAll());

            // Step 5: Acknowledge incident 1
            System.out.println("\n--- Acknowledging Incident 1 ---");
            boolean ack1 = dao.acknowledge(id1, "Alice");
            System.out.println("Incident 1 acknowledged: " + ack1);

            // Step 6: Resolve incident 1
            System.out.println("\n--- Resolving Incident 1 ---");
            boolean res1 = dao.resolve(id1);
            System.out.println("Incident 1 resolved: " + res1);
            System.out.println("Incident 1 current state: " + dao.findById(id1));

            // Step 7: Try to resolve incident 2 directly without acknowledging it first
            System.out.println("\n--- Attempting to Resolve Incident 2 Directly (Expected to fail) ---");
            boolean res2 = dao.resolve(id2);
            System.out.println("Incident 2 resolved directly: " + res2 + " (Failed because current status is OPEN, not ACKNOWLEDGED)");

            // Step 8: Print incidents with status "OPEN"
            System.out.println("\n--- Incidents with Status OPEN ---");
            printIncidents(dao.findByStatus(Incident.STATUS_OPEN));

            // Step 9: Delete incident 3
            System.out.println("\n--- Deleting Incident 3 ---");
            boolean del3 = dao.delete(id3);
            System.out.println("Incident 3 deleted: " + del3);

            // Step 10: Print all incidents again to verify final state
            System.out.println("\n--- All Incidents (Final State) ---");
            printIncidents(dao.findAll());

        } catch (SQLException e) {
            System.err.println("Database operation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper method to display a list of incidents
    private static void printIncidents(List<Incident> incidents) {
        if (incidents.isEmpty()) {
            System.out.println("  (None found)");
        } else {
            for (Incident incident : incidents) {
                System.out.println("  " + incident);
            }
        }
    }
}
