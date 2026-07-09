package dev.aegiseye.gateway.ingest;

public class InvalidEvidenceException extends RuntimeException {
    public InvalidEvidenceException(String message) {
        super(message);
    }

    public InvalidEvidenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
