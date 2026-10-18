package loglens.exception;

// Custom checked exception thrown when a log line cannot be parsed due to invalid format.
// Why a custom exception is useful:
// 1. It provides a clear, specific name for this exact error rather than using a generic Exception.
// 2. It allows calling code to catch MalformedLogException specifically and decide how to handle it (e.g., log a warning and continue reading the rest of the file).
// 3. It carries extra context—in this case, the raw text of the broken line—making it much easier to debug.
public class MalformedLogException extends Exception {

    // Stores the original, unparseable line of text from the log file
    private final String rawLine;

    // Constructor: forwards the error message to the parent Exception class with super(message)
    // and saves the broken line in rawLine
    public MalformedLogException(String message, String rawLine) {
        super(message);
        this.rawLine = rawLine;
    }

    // Getter to access the broken log line that caused this exception
    public String getRawLine() {
        return rawLine;
    }
}
