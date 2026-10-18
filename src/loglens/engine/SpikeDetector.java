package loglens.engine;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;

import loglens.model.LogEntry;

// Detects sudden spikes in errors for microservices using a sliding time window.
//
// What is a Sliding Window?
// Imagine looking at log events through a moving frame (e.g., the last 10 seconds).
// Whenever a new error arrives, we add its timestamp to the window. As time moves forward,
// any older timestamps that fall outside the window are removed from the front of the list.
// If the number of remaining timestamps reaches our threshold, we trigger an alert.
public class SpikeDetector {

    // The length of our sliding time window in seconds (e.g., 10 seconds)
    private final int windowSeconds;

    // The error count required within the window to trigger a spike alert (e.g., 3 errors)
    private final int threshold;

    // Stores the list of recent error timestamps for each service.
    // LinkedList is ideal here because adding to the end and removing expired timestamps
    // from the front are both fast O(1) operations.
    private final HashMap<String, LinkedList<LocalDateTime>> errorWindows;

    // Tracks whether a service is currently in a spike state.
    // This prevents sending duplicate alert spam for every subsequent error during the same spike.
    private final HashMap<String, Boolean> inSpike;

    // Constructor: sets the window size and alert threshold
    public SpikeDetector(int windowSeconds, int threshold) {
        this.windowSeconds = windowSeconds;
        this.threshold = threshold;
        this.errorWindows = new HashMap<String, LinkedList<LocalDateTime>>();
        this.inSpike = new HashMap<String, Boolean>();
    }

    // Records an incoming log entry and returns true if a NEW error spike is detected.
    public boolean record(LogEntry entry) {
        // Step 1: If entry is null or not an ERROR, it doesn't count towards an error spike
        if (entry == null || !entry.isError()) {
            return false;
        }

        String service = entry.getService();

        // Step 2: Retrieve (or initialize) the list of error timestamps for this service
        LinkedList<LocalDateTime> timestamps = errorWindows.get(service);
        if (timestamps == null) {
            timestamps = new LinkedList<LocalDateTime>();
            errorWindows.put(service, timestamps);
        }

        // Step 3: Add the new error's timestamp to the back of the queue
        timestamps.add(entry.getTimestamp());

        // Step 4: Slide the window.
        // Compute the cutoff time (newest timestamp minus windowSeconds).
        // Remove any timestamps from the front that are older than the window.
        LocalDateTime cutoff = entry.getTimestamp().minusSeconds(windowSeconds);
        while (!timestamps.isEmpty() && timestamps.getFirst().isBefore(cutoff)) {
            timestamps.removeFirst();
        }

        // Step 5: Check if the number of errors meets or exceeds our threshold
        boolean alreadyInSpike = inSpike.containsKey(service) && inSpike.get(service);

        if (timestamps.size() >= threshold) {
            // Trigger an alert only if the service wasn't already marked as in a spike
            if (!alreadyInSpike) {
                inSpike.put(service, true);
                return true;
            }
        } else {
            // If the error count dropped below threshold, reset the spike state
            inSpike.put(service, false);
        }

        return false;
    }

    // Returns the current number of active errors in the sliding window for a given service.
    // Returns 0 if the service has no recorded errors.
    public int getErrorCount(String service) {
        LinkedList<LocalDateTime> timestamps = errorWindows.get(service);
        return (timestamps != null) ? timestamps.size() : 0;
    }
}
