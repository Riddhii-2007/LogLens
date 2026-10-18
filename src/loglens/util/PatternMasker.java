package loglens.util;

// Utility class that masks dynamic values (like IP addresses and numeric IDs) in log messages.
//
// What is masking and why is it needed?
// Log messages usually include dynamic runtime data such as IP addresses, user IDs, or order numbers:
//   "Timeout connecting to DB at 10.0.0.4"
//   "Timeout connecting to DB at 10.0.0.7"
// To a computer, these two strings look different. By masking dynamic tokens with generic placeholders:
//   "Timeout connecting to DB at <IP>"
// we can group hundreds of similar log events into a single root pattern for easy aggregation.
public class PatternMasker {

    // Regex pattern matching standard IPv4 addresses (e.g., 10.0.0.4 or 192.168.1.1)
    private static final String IP_REGEX = "\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b";

    // Regex pattern matching any remaining sequence of digits (e.g., 503, 10042)
    private static final String DIGIT_REGEX = "\\d+";

    // Private constructor to prevent instantiation since all methods are static
    private PatternMasker() {
    }

    // Replaces IP addresses with <IP> and remaining numbers with <N>.
    // Returns an empty string if the input message is null.
    public static String mask(String message) {
        if (message == null) {
            return "";
        }

        // Why does the order of replacements matter?
        // An IP address consists of numbers separated by dots (e.g. 10.0.0.4).
        // If we replaced digits first, 10.0.0.4 would become "<N>.<N>.<N>.<N>",
        // which completely ruins the structure of the IP address so the IP regex would never match it!
        // Therefore, we MUST mask full IP addresses first, and only then mask any remaining individual numbers.
        String maskedIps = message.replaceAll(IP_REGEX, "<IP>");
        return maskedIps.replaceAll(DIGIT_REGEX, "<N>");
    }
}
