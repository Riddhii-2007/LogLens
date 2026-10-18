package loglens.parser;

import loglens.model.LogEntry;
import loglens.exception.MalformedLogException;

// What is an abstract class?
// An abstract class serves as a foundational blueprint that cannot be instantiated directly with 'new'.
// It is used to share common code across related classes while enforcing a structure for child classes to follow.
public abstract class LogParser {

    // Why does parse() have no body?
    // Different log formats (such as space-separated text, JSON, or CSV) require different parsing logic.
    // By leaving parse() abstract (with a semicolon instead of curly braces {}), we define a required contract:
    // any concrete subclass that extends LogParser MUST provide its own specific parsing implementation.
    public abstract LogEntry parse(String line) throws MalformedLogException;

    // Why does isValidLevel() live here instead of in the subclass?
    // Checking whether a log level is valid ("INFO", "WARN", or "ERROR") is a shared business rule across all parsers.
    // By keeping it in this parent class with 'protected' visibility, every subclass can reuse it without duplicating code.
    protected boolean isValidLevel(String level) {
        return "INFO".equals(level) || "WARN".equals(level) || "ERROR".equals(level);
    }
}
