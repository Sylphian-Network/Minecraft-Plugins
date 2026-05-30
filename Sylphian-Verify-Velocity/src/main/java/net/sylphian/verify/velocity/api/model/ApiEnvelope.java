package net.sylphian.verify.velocity.api.model;

import java.util.List;
import java.util.Map;

/**
 * Standard API response envelope used across the Sylphian network.
 * Wraps the primary data payload with status, messages, and error details.
 *
 * @param <T> the type of the contained data
 */
public class ApiEnvelope<T> {
    /** Whether the API request was successful. */
    private boolean success;
    /** The primary data payload of the response. */
    private T data;
    /** An optional message accompanying the response. */
    private String message;
    /** Detailed error messages, usually for validation failures. */
    private Map<String, List<String>> errors;
    /** Metadata about the request or response. */
    private Map<String, Object> meta;

    /**
     * Checks if the request failed.
     * @return true if success is false, false otherwise
     */
    public boolean isFailed() {
        return !success;
    }

    /**
     * Gets the data payload.
     * @return the data object
     */
    public T getData() {
        return data;
    }

    /**
     * Gets the response message.
     * @return the message string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets any error details.
     * @return a map of error lists
     */
    public Map<String, List<String>> getErrors() {
        return errors;
    }

    /**
     * Gets the response metadata.
     * @return a map of metadata
     */
    public Map<String, Object> getMeta() {
        return meta;
    }
}
