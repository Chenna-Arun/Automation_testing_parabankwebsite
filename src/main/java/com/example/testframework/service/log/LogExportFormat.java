package com.example.testframework.service.log;

/**
 * Log export formats
 */
public enum LogExportFormat {
    JSON("application/json"),
    CSV("text/csv"),
    TXT("text/plain"),
    XML("application/xml");

    private final String mimeType;

    LogExportFormat(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }
}