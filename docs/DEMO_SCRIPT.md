# LogLens Demo Script & Viva Guide

---

## Part 1: Step-by-Step 2 to 3 Minute Demo Script

Follow this numbered script during your project presentation or demonstration:

1. **Launch & Login as Administrator (0:00 - 0:25)**
   - Launch the application by double-clicking `run.bat` (or executing it from the terminal).
   - Point out the centered **Login** card rendered via `CardLayout`.
   - Enter credentials:
     - **Username:** `admin`
     - **Password:** `admin123`
     - *(Press Enter in the password field to demonstrate keyboard navigation)*.
   - Point out the transition to the **Dashboard** card and the status line at the top: `Logged in as admin (ADMIN)`.

2. **Normal Traffic & Live Log Filtering (0:25 - 0:50)**
   - At the bottom toolbar, click **Normal traffic (20 lines)**.
   - Under the **Live Logs** tab, show valid log entries appearing and scrolling in real time.
   - Point out the custom table cell rendering: `INFO` lines have clean white backgrounds, and `WARN` lines are highlighted in subtle yellow.
   - Type `payment` in the **Search** box at the top to demonstrate dynamic regex row filtering; clear the box to restore the full view.

3. **Triggering Error Bursts & Pinned Spike Alerts (0:50 - 1:15)**
   - Click **Error burst: payment-service (12)** at the bottom.
   - Highlight the top status line: it immediately displays a prominent red alert:
     `[ALERT] SPIKE ALERT: payment-service (12 errors in the last 10 seconds)`.
   - Explain that spike alerts are **pinned**—subsequent normal traffic will not overwrite this warning.

4. **Incident Lifecycle & Dynamic Escalation (1:15 - 1:45)**
   - Switch to the **Incidents** tab.
   - Show the newly generated incident row for `payment-service`.
   - Point out:
     - **Severity:** Marked as `CRITICAL` (colored light red) because the error frequency exceeded 10 in the sliding window.
     - **Status:** Initialized to `OPEN`.
   - In the **Handled by** text field, enter your name (e.g., `Alice`).
   - Select the incident row and click **Acknowledge**.
     - Notice the status immediately changes to `ACKNOWLEDGED`, recording the timestamp and operator name in SQLite.
   - Click **Resolve**.
     - Notice the status updates to `RESOLVED`, completing the incident state machine and unblocking future alerts for that service in `IncidentManager`.

5. **Error Signature Grouping (1:45 - 2:05)**
   - Click the **Patterns** tab.
   - Show the aggregated error table.
   - Explain how `PatternMasker` converted dynamic variables (like IPs and IDs) into `<IP>` and `<N>`, grouping repeated failures into unified signatures with accurate hit counts.

6. **Skipped Lines & Parser Error Handling (2:05 - 2:25)**
   - At the bottom toolbar, click **Garbage lines**.
   - Notice the status bar mentions malformed lines were received, and look at the fourth tab: its title dynamically updates to **`Skipped Lines (3)`**.
   - Switch to the **Skipped Lines** tab to show the audit table displaying:
     - Timestamp
     - Parser rejection reason (`Missing pipe delimiters`, `Invalid timestamp`, etc.)
     - Exact raw line content
   - Explain that corrupted lines are audited separately so production errors are never lost, without corrupting the clean structured log table.

7. **Exporting Incidents to CSV (2:25 - 2:40)**
   - Go back to the **Incidents** tab and click **Export CSV**.
   - Point out the confirmation dialog indicating the CSV was generated in the `exports/` folder.

8. **Logout & Role-Based Access Control (2:40 - 3:00)**
   - Click **Logout** in the bottom right corner. Point out that the session clears and flips back to the login screen.
   - Log in using the read-only viewer credentials:
     - **Username:** `viewer`
     - **Password:** `viewer123`
   - Point out the status bar: `Logged in as viewer (VIEWER)`.
   - Switch to the **Incidents** tab:
     - Show that the **Acknowledge** button, **Resolve** button, and **Handled by** field are completely disabled (`setReadOnly(true)`).
   - Conclude by highlighting this role-based authorization mechanism.

---

## Part 2: Top 8 Likely Viva Questions & Answers

### 1. Why do you use an interface (`LogListener`) instead of calling UI methods directly from the watcher?
> **Answer:** Using the `LogListener` interface adheres to the **Observer Pattern** and the **Open/Closed Principle**. The `LogWatcher` doesn't need to know anything about Swing, databases, or UI components—it simply emits events to whatever listener is attached. This allows us to chain listeners (e.g., `LogWatcher` &rarr; `IncidentManager` &rarr; UI listener) or swap the GUI with a CLI or test mock without altering the ingestion engine.

### 2. Why is the `running` flag marked `volatile` in `LogWatcher`?
> **Answer:** In multithreaded Java, different threads often run on different CPU cores and keep cached copies of shared variables in CPU registers. The `volatile` keyword guarantees **memory visibility**: when the main/UI thread sets `running = false` via `stop()`, the CPU memory cache is immediately flushed and invalidated, ensuring the background `LogWatcher` thread sees the updated value instantly on its next loop iteration.

### 3. Why are methods in `SpikeDetector` and `ErrorGrouper` marked `synchronized`?
> **Answer:** To prevent **race conditions** and protect shared state. The background `LogWatcher` thread constantly calls `record()` to mutate internal data structures (`ArrayDeque` timestamps and `HashMap` counts), while the Swing Event Dispatch Thread (EDT) concurrently reads from these same maps to update tables and charts. Marking these methods `synchronized` ensures only one thread can access or modify the collections at any time.

### 4. What is the difference between `Statement` and `PreparedStatement`, and why is `PreparedStatement` preferred in your DAOs?
> **Answer:** A `Statement` takes raw SQL strings concatenated with inputs, leaving the application vulnerable to **SQL Injection** attacks. A `PreparedStatement` pre-compiles the SQL query structure on the database engine and safely binds user inputs as parameters (`?`), treating them purely as literal values. It also improves execution speed for repeated queries.

### 5. Why is the status check (`status IN ('OPEN', 'ACKNOWLEDGED')`) performed in the SQL `WHERE` clause rather than filtering in Java?
> **Answer:** Querying with a SQL `WHERE` clause leverages the database engine's query optimizer and indexes, fetching only matching records over the JDBC connection. If we filtered in Java, we would have to load thousands of historical `RESOLVED` incidents into memory, causing unnecessary heap memory consumption, network latency, and CPU overhead.

### 6. Why must UI modifications be wrapped inside `SwingUtilities.invokeLater()`?
> **Answer:** Java Swing is single-threaded and not thread-safe. All modifications to Swing components (adding table rows, updating labels, changing tabs) must happen strictly on the **Event Dispatch Thread (EDT)**. Since log events arrive on a background thread (`LogWatcher`), calling UI methods directly causes race conditions, visual artifacts, and thread deadlocks. `invokeLater` pushes the update task onto the EDT queue to be executed safely.

### 7. Why are log messages masked by `PatternMasker` before being recorded by `ErrorGrouper`?
> **Answer:** In distributed systems, log messages frequently contain dynamic, transient variables like IP addresses, transaction numbers, and user IDs (e.g., `Connection refused from 192.168.1.15 on port 8080` vs `Connection refused from 10.0.0.4 on port 8080`). `PatternMasker` replaces IPs with `<IP>` and numbers with `<N>`, normalizing them to a single pattern signature (`Connection refused from <IP> on port <N>`). This allows `ErrorGrouper` to accurately aggregate duplicate errors and identify top root causes.

### 8. Why do we store a cryptographic hash (SHA-256) instead of plaintext passwords in SQLite?
> **Answer:** To ensure **defense in depth**. If the SQLite database file (`loglens.db`) is stolen, leaked, or inspected, plaintext passwords would compromise all user accounts and any external accounts sharing those passwords. Cryptographic hashes are mathematically one-way: passwords cannot be reverse-engineered from the hash. During login, the entered password is hashed and compared to the stored hash.
