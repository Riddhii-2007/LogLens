package loglens.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import loglens.model.LogEntry;
import loglens.util.PatternMasker;

// Aggregates recurring error messages into common patterns and counts their frequencies.
public class ErrorGrouper {

    // Maps each unique pattern key ("<service>: <masked_message>") to the number of times it has occurred
    private final HashMap<String, Integer> patternCounts;

    // Constructor: initializes the pattern map
    public ErrorGrouper() {
        this.patternCounts = new HashMap<String, Integer>();
    }

    // Why are these methods marked 'synchronized'?
    // In our live system, the background LogWatcher thread writes to patternCounts as log lines arrive,
    // while the main thread might read from it at the same time to display top patterns.
    // Making methods 'synchronized' provides thread safety: only one thread can access or modify
    // the internal HashMap at a time, preventing data corruption or race conditions.
    public synchronized void record(LogEntry entry) {
        // Only process valid ERROR entries
        if (entry == null || !entry.isError()) {
            return;
        }

        // Mask variable details (IPs, numbers) so identical issues group together
        String maskedPattern = PatternMasker.mask(entry.getMessage());

        // Prefix with service name so errors with the same message across different services stay distinct
        String key = entry.getService() + ": " + maskedPattern;

        Integer count = patternCounts.get(key);
        if (count == null) {
            patternCounts.put(key, 1);
        } else {
            patternCounts.put(key, count + 1);
        }
    }

    // How does the sorting work?
    // A HashMap does not maintain any sorted order. To sort by count (highest first):
    // 1. We copy all Map.Entry objects into a new ArrayList.
    // 2. We use Collections.sort with an anonymous inner class Comparator.
    // 3. Comparing 'o2.getValue().compareTo(o1.getValue())' orders them in descending order (highest count first).
    public synchronized List<Map.Entry<String, Integer>> getTopPatterns() {
        List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(patternCounts.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                // Descending order: highest count comes first
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        return entries;
    }

    // Returns the masked pattern with the highest count for the given service (excluding "service: " prefix),
    // or "Unknown error" if no error pattern exists for that service.
    public synchronized String getTopPatternForService(String service) {
        if (service == null) {
            return "Unknown error";
        }

        String prefix = service + ": ";
        String topPattern = null;
        int maxCount = -1;

        for (Map.Entry<String, Integer> entry : patternCounts.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    topPattern = entry.getKey().substring(prefix.length());
                }
            }
        }

        return (topPattern != null) ? topPattern : "Unknown error";
    }

    // Returns the total number of distinct error patterns observed so far
    public synchronized int getPatternCount() {
        return patternCounts.size();
    }
}



