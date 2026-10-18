package loglens;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Properties;

import loglens.dao.IncidentDAO;
import loglens.model.Incident;

// =============================================================================
// LogLens CallableStatement Demonstration (MySQL)
// =============================================================================
//
// What is a Stored Procedure?
// A stored procedure is a pre-compiled collection of SQL statements stored directly
// inside the database server engine. Instead of sending complex or repetitive SQL
// strings over the network, an application calls the stored procedure by name.
//
// What is CallableStatement?
// CallableStatement is the JDBC interface (extending PreparedStatement) used to
// execute stored procedures. It supports passing input values and reading returned
// output parameters or ResultSets from the procedure.
//
// Understanding Parameter Types:
// - IN parameter: Supplies an input value from Java to the stored procedure.
//   Set using standard setter methods like cs.setString(index, value).
// - OUT parameter: Returns a calculated or queried value from the stored procedure
//   back to Java. It MUST be registered prior to execution with registerOutParameter()
//   and read after execution using getter methods like cs.getInt(index).
// - INOUT parameter: Acts as both an input and an output. Java provides an initial
//   value (via setter), the procedure processes and mutates it, and Java retrieves
//   the updated value (via getter). It must be BOTH set and registered.
//
// Why is registerOutParameter() needed?
// JDBC drivers must know the target SQL type (from java.sql.Types) beforehand so
// the driver can allocate appropriate memory buffers and handle datatype conversions
// when the database engine sends the output value back.
//
// Why does JDBC use the {call procedure_name(?, ?)} escape syntax?
// The curly-brace escape syntax {call ...} is the vendor-neutral SQL standard
// defined by JDBC. Each database engine has its own native syntax (e.g. "CALL" in
// MySQL vs "EXEC" in SQL Server). The JDBC driver intercepts the {call ...}
// syntax and translates it into the appropriate native database command.
// =============================================================================
public class CallableDemo {

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("        LogLens CallableStatement Demo (MySQL Stored Procedures) ");
        System.out.println("=================================================================");

        // Step 1: Load database credentials from mysql.properties
        File configFile = new File("mysql.properties");
        if (!configFile.exists()) {
            System.err.println("\n[ERROR] Configuration file 'mysql.properties' not found!");
            System.err.println("To run this demo:");
            System.err.println("  1. Copy 'mysql.properties.example' to 'mysql.properties'.");
            System.err.println("  2. Open 'mysql.properties' and enter your local MySQL password.");
            System.err.println("  3. Ensure your MySQL server is running on localhost:3306.");
            System.err.println("  4. Run this demo again.\n");
            return;
        }

        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            config.load(fis);
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to read 'mysql.properties': " + e.getMessage());
            return;
        }

        String url = config.getProperty("url");
        String user = config.getProperty("user");
        String password = config.getProperty("password");

        if (url == null || user == null || password == null) {
            System.err.println("[ERROR] 'mysql.properties' must specify 'url', 'user', and 'password'.");
            return;
        }

        // Explicitly check for MySQL driver class availability (gives clear error if jar missing)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("\n[ERROR] MySQL JDBC driver not found on classpath!");
            System.err.println("Make sure 'lib/mysql-connector-j-8.0.33.jar' is included in your -cp argument.");
            System.err.println("Example: java -cp \"bin;lib/sqlite-jdbc-3.36.0.3.jar;lib/mysql-connector-j-8.0.33.jar\" loglens.CallableDemo\n");
            return;
        }

        // Step 2: Connect to MySQL and run setup & demonstrations
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("[INFO] Successfully connected to MySQL at: " + url);

            // Setup schema and stored procedures
            setupDatabaseAndProcedures(conn);

            // Populate table if empty
            ensureDataPopulated(conn);

            // Demo Step 1: IN and OUT parameters
            runDemoStep1InOutParams(conn);

            // Demo Step 2: ResultSet returned from Stored Procedure
            runDemoStep2ResultSet(conn);

            // Demo Step 3: INOUT parameter
            runDemoStep3InOutParameter(conn);

            System.out.println("\n=================================================================");
            System.out.println("  CallableStatement Demonstration Completed Successfully!");
            System.out.println("=================================================================\n");

        } catch (SQLException e) {
            handleSqlException(e);
        }
    }

    // Creates the incidents table and stored procedures in MySQL using standard Statement
    private static void setupDatabaseAndProcedures(Connection conn) throws SQLException {
        System.out.println("\n[SETUP] Initializing MySQL tables and stored procedures...");

        try (Statement stmt = conn.createStatement()) {
            // Table definition
            stmt.execute("CREATE TABLE IF NOT EXISTS incidents ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT, "
                    + "service VARCHAR(100) NOT NULL, "
                    + "severity VARCHAR(20) NOT NULL, "
                    + "status VARCHAR(20) NOT NULL"
                    + ")");

            // Stored Procedure a: incident_count_by_status (Demonstrates IN and OUT)
            stmt.execute("DROP PROCEDURE IF EXISTS incident_count_by_status");
            stmt.execute("CREATE PROCEDURE incident_count_by_status("
                    + "IN p_status VARCHAR(20), "
                    + "OUT p_count INT"
                    + ") "
                    + "BEGIN "
                    + "  SELECT COUNT(*) INTO p_count FROM incidents WHERE status = p_status; "
                    + "END");

            // Stored Procedure b: incidents_by_severity (Demonstrates ResultSet)
            stmt.execute("DROP PROCEDURE IF EXISTS incidents_by_severity");
            stmt.execute("CREATE PROCEDURE incidents_by_severity("
                    + "IN p_severity VARCHAR(20)"
                    + ") "
                    + "BEGIN "
                    + "  SELECT id, service, severity, status FROM incidents WHERE severity = p_severity ORDER BY id ASC; "
                    + "END");

            // Stored Procedure c: bump_severity (Demonstrates INOUT)
            stmt.execute("DROP PROCEDURE IF EXISTS bump_severity");
            stmt.execute("CREATE PROCEDURE bump_severity("
                    + "INOUT p_severity VARCHAR(20)"
                    + ") "
                    + "BEGIN "
                    + "  IF p_severity = 'MEDIUM' THEN "
                    + "    SET p_severity = 'HIGH'; "
                    + "  ELSEIF p_severity = 'HIGH' THEN "
                    + "    SET p_severity = 'CRITICAL'; "
                    + "  END IF; "
                    + "END");
        }
        System.out.println("[SETUP] Table and 3 stored procedures created successfully.");
    }

    // Ensures the incidents table has data by copying from SQLite or inserting 6 fallback sample rows
    private static void ensureDataPopulated(Connection conn) throws SQLException {
        boolean isEmpty = true;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM incidents")) {
            if (rs.next() && rs.getInt(1) > 0) {
                isEmpty = false;
                System.out.println("[DATA] Table 'incidents' already contains " + rs.getInt(1) + " records.");
            }
        }

        if (!isEmpty) {
            return;
        }

        // Try to copy real incidents from SQLite via IncidentDAO
        boolean copiedFromSqlite = false;
        try {
            IncidentDAO sqliteDao = new IncidentDAO();
            List<Incident> sqliteIncidents = sqliteDao.findAll();
            if (sqliteIncidents != null && !sqliteIncidents.isEmpty()) {
                String insertSql = "INSERT INTO incidents (service, severity, status) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    for (Incident inc : sqliteIncidents) {
                        ps.setString(1, inc.getService());
                        ps.setString(2, inc.getSeverity());
                        ps.setString(3, inc.getStatus());
                        ps.executeUpdate();
                    }
                }
                System.out.println("[DATA] Copied " + sqliteIncidents.size() + " incidents from SQLite into MySQL.");
                copiedFromSqlite = true;
            }
        } catch (Exception e) {
            // SQLite might be empty or unavailable; fall through to sample insertion
        }

        // If SQLite had no data or failed, insert 6 sample records
        if (!copiedFromSqlite) {
            String insertSql = "INSERT INTO incidents (service, severity, status) VALUES (?, ?, ?)";
            String[][] sampleRows = {
                {"payment-service", "CRITICAL", "OPEN"},
                {"auth-service", "HIGH", "OPEN"},
                {"order-service", "MEDIUM", "ACKNOWLEDGED"},
                {"inventory-service", "MEDIUM", "ACKNOWLEDGED"},
                {"notification-service", "HIGH", "RESOLVED"},
                {"payment-service", "CRITICAL", "RESOLVED"}
            };

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (String[] row : sampleRows) {
                    ps.setString(1, row[0]);
                    ps.setString(2, row[1]);
                    ps.setString(3, row[2]);
                    ps.executeUpdate();
                }
            }
            System.out.println("[DATA] Inserted 6 sample records into MySQL incidents table.");
        }
    }

    // Demo Step 1: Stored procedure with IN parameter and OUT parameter
    private static void runDemoStep1InOutParams(Connection conn) throws SQLException {
        System.out.println("\n-----------------------------------------------------------------");
        System.out.println("Demo 1: IN and OUT Parameters (incident_count_by_status)");
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Calling procedure: {call incident_count_by_status(?, ?)}");

        String[] statuses = {"OPEN", "ACKNOWLEDGED", "RESOLVED"};

        // Notice the JDBC escape syntax {call procedure_name(?, ?)}
        try (CallableStatement cs = conn.prepareCall("{call incident_count_by_status(?, ?)}")) {
            for (String status : statuses) {
                // Parameter 1 is an IN parameter: set value from Java
                cs.setString(1, status);

                // Parameter 2 is an OUT parameter: register its SQL type prior to execution
                cs.registerOutParameter(2, Types.INTEGER);

                // Execute the stored procedure
                cs.execute();

                // Retrieve the OUT parameter value using the appropriate getter
                int count = cs.getInt(2);
                System.out.printf("  Status: %-15s -> Count: %d%n", status, count);
            }
        }
    }

    // Demo Step 2: Stored procedure returning a ResultSet
    private static void runDemoStep2ResultSet(Connection conn) throws SQLException {
        System.out.println("\n-----------------------------------------------------------------");
        System.out.println("Demo 2: Stored Procedure returning a ResultSet (incidents_by_severity)");
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Calling procedure: {call incidents_by_severity(?)} with severity 'CRITICAL'");

        try (CallableStatement cs = conn.prepareCall("{call incidents_by_severity(?)}")) {
            cs.setString(1, "CRITICAL");

            try (ResultSet rs = cs.executeQuery()) {
                System.out.printf("  %-6s | %-22s | %-10s | %-15s%n", "ID", "SERVICE", "SEVERITY", "STATUS");
                System.out.println("  -------------------------------------------------------------");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    int id = rs.getInt("id");
                    String service = rs.getString("service");
                    String severity = rs.getString("severity");
                    String status = rs.getString("status");
                    System.out.printf("  %-6d | %-22s | %-10s | %-15s%n", id, service, severity, status);
                }
                if (!found) {
                    System.out.println("  (No CRITICAL incidents found)");
                }
            }
        }
    }

    // Demo Step 3: Stored procedure with an INOUT parameter
    private static void runDemoStep3InOutParameter(Connection conn) throws SQLException {
        System.out.println("\n-----------------------------------------------------------------");
        System.out.println("Demo 3: INOUT Parameter (bump_severity)");
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Calling procedure: {call bump_severity(?)}");

        try (CallableStatement cs = conn.prepareCall("{call bump_severity(?)}")) {
            String initialSeverity = "MEDIUM";

            // An INOUT parameter requires BOTH setting the input value and registering the OUT type
            cs.setString(1, initialSeverity);
            cs.registerOutParameter(1, Types.VARCHAR);

            System.out.println("  Value before stored procedure execution: " + initialSeverity);

            // Execute the procedure on the database server
            cs.execute();

            // Read back the updated/mutated value
            String updatedSeverity = cs.getString(1);
            System.out.println("  Value after stored procedure execution:  " + updatedSeverity);
        }
    }

    // Prints beginner-friendly explanations for common connection and database errors
    private static void handleSqlException(SQLException e) {
        System.err.println("\n[SQL ERROR] " + e.getMessage());

        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String sqlState = e.getSQLState() != null ? e.getSQLState() : "";

        if (message.contains("communications link failure") || message.contains("connection refused")) {
            System.err.println("\n>>> DIAGNOSIS: Cannot connect to MySQL server.");
            System.err.println("    - Is MySQL installed and currently running?");
            System.err.println("    - Check Windows Services (services.msc) to verify 'MySQL' service is started.");
            System.err.println("    - Verify the port in 'mysql.properties' is correct (default: 3306).");
        } else if (message.contains("access denied") || e.getErrorCode() == 1045 || "28000".equals(sqlState)) {
            System.err.println("\n>>> DIAGNOSIS: Access denied for MySQL user.");
            System.err.println("    - The password or username in 'mysql.properties' is incorrect.");
            System.err.println("    - Check 'user' and 'password' in 'mysql.properties' and try again.");
        } else if (message.contains("no suitable driver")) {
            System.err.println("\n>>> DIAGNOSIS: MySQL JDBC driver is not on the classpath.");
            System.err.println("    - Include 'lib/mysql-connector-j-8.0.33.jar' in your java -cp command.");
        } else {
            System.err.println("\n>>> Error Code: " + e.getErrorCode() + " | SQLState: " + sqlState);
        }
        System.err.println();
    }
}
