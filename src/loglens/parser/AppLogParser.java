package loglens.parser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.StringTokenizer;

import loglens.model.LogEntry;
import loglens.exception.MalformedLogException;

// Concrete implementation of LogParser designed to parse standard application log lines.
// Inherits common methods like isValidLevel() from the abstract parent class LogParser.
public class AppLogParser extends LogParser {

    // DateTimeFormatter defining the expected timestamp layout: "yyyy-MM-dd HH:mm:ss"
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Parses a single raw log line into a strongly typed LogEntry object.
    // If the line format is invalid, it throws a MalformedLogException.
    @Override
    public LogEntry parse(String line) throws MalformedLogException {
        // Step 1: Validate that the input line is neither null nor purely whitespace.
        // We throw MalformedLogException early to prevent NullPointerException later.
        if (line == null || line.trim().isEmpty()) {
            throw new MalformedLogException("Log line is null or empty", line);
        }

        // Step 2: Use StringTokenizer to split the line by whitespace into tokens.
        // A valid line must have at least 5 tokens: date, time, level, service, and message.
        StringTokenizer tokenizer = new StringTokenizer(line);
        if (tokenizer.countTokens() < 5) {
            throw new MalformedLogException("Log line has fewer than 5 tokens", line);
        }

        // Step 3: Extract the first four known tokens in sequence.
        String datePart = tokenizer.nextToken();
        String timePart = tokenizer.nextToken();
        String level = tokenizer.nextToken();
        String service = tokenizer.nextToken();

        // Step 4: Validate the log level using the parent method isValidLevel().
        // Must be exactly "INFO", "WARN", or "ERROR" (case-sensitive).
        if (!isValidLevel(level)) {
            throw new MalformedLogException("Invalid log level: " + level, line);
        }

        // Step 5: Parse the date and time strings into a LocalDateTime object.
        // If the date/time is invalid (e.g. 2026-13-45 or 99:99:99), catch DateTimeParseException
        // and convert it to our custom MalformedLogException.
        LocalDateTime timestamp;
        try {
            timestamp = LocalDateTime.parse(datePart + " " + timePart, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new MalformedLogException("Invalid date/time format: " + datePart + " " + timePart, line);
        }

        // Step 6: Collect all remaining tokens to reconstruct the log message.
        // Since messages can contain spaces, we join the remaining tokens with a space.
        StringBuilder messageBuilder = new StringBuilder();
        while (tokenizer.hasMoreTokens()) {
            if (messageBuilder.length() > 0) {
                messageBuilder.append(" ");
            }
            messageBuilder.append(tokenizer.nextToken());
        }
        String message = messageBuilder.toString();

        // Step 7: Return a new, fully populated LogEntry object.
        return new LogEntry(timestamp, level, service, message);
    }
}
