# LogLens Demo Script

---

## Step-by-Step 2 to 3 Minute Demo Walkthrough

Follow this numbered script during your project walkthrough or feature demonstration:

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
   - Type `payment` in the **Search** box at the top to demonstrate case-insensitive text search across all columns; clear the box to restore the full view.

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
