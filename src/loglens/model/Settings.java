package loglens.model;

import java.io.Serializable;

// Model class representing application configuration settings.
// Implements Serializable so settings can be persisted to disk using Java Object Serialization.
public class Settings implements Serializable {

    private static final long serialVersionUID = 1L;

    // Sliding time window in seconds for spike detection
    private int windowSeconds = 10;

    // Error count threshold required to trigger an alert
    private int threshold = 3;

    // Target folder name for exported incident CSV files
    private String exportFolder = "exports";

    // Default constructor
    public Settings() {
    }

    // Overloaded constructor with initial values
    public Settings(int windowSeconds, int threshold, String exportFolder) {
        this.windowSeconds = windowSeconds;
        this.threshold = threshold;
        this.exportFolder = exportFolder;
    }

    public int getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public String getExportFolder() {
        return exportFolder;
    }

    public void setExportFolder(String exportFolder) {
        this.exportFolder = exportFolder;
    }

    @Override
    public String toString() {
        return "Settings [windowSeconds=" + windowSeconds + ", threshold=" + threshold 
                + ", exportFolder=" + exportFolder + "]";
    }
}
