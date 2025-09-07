package com.example.testframework.model;

/**
 * Enumeration of log levels in order of severity
 */
public enum LogLevel {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3),
    FATAL(4);

    private final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }

    public boolean isMoreSevereThan(LogLevel other) {
        return this.severity > other.severity;
    }

    public boolean isLessOrEqualSeverityThan(LogLevel other) {
        return this.severity <= other.severity;
    }

    public static LogLevel fromString(String level) {
        if (level == null) {
            return INFO;
        }
        
        try {
            return LogLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return INFO; // Default to INFO if unknown level
        }
    }

    public boolean isDebugEnabled() {
        return this == DEBUG;
    }

    public boolean isInfoEnabled() {
        return this.severity >= INFO.severity;
    }

    public boolean isWarnEnabled() {
        return this.severity >= WARN.severity;
    }

    public boolean isErrorEnabled() {
        return this.severity >= ERROR.severity;
    }
}