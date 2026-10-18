package loglens.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import loglens.model.Settings;

// Utility class responsible for saving and loading application Settings using Java Serialization.
public class SettingsStore {

    private static final String SETTINGS_FILE = "settings.ser";

    private SettingsStore() {
    }

    // Loads Settings from "settings.ser". If the file does not exist or is corrupted,
    // catches IOException and ClassNotFoundException and safely returns a new default Settings instance.
    public static Settings load() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists()) {
            return new Settings();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof Settings) {
                return (Settings) obj;
            }
        } catch (IOException e) {
            System.err.println("Could not read settings from " + SETTINGS_FILE + " (using defaults): " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Settings class definition not found (using defaults): " + e.getMessage());
        }

        return new Settings();
    }

    // Persists the given Settings object to "settings.ser"
    public static void save(Settings s) {
        if (s == null) {
            return;
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SETTINGS_FILE))) {
            oos.writeObject(s);
        } catch (IOException e) {
            System.err.println("Error saving settings to " + SETTINGS_FILE + ": " + e.getMessage());
        }
    }
}
