package net.sylphian.minecraft.verify.api.model;

import java.util.List;
import java.util.Map;

/**
 * Represents a standard API response envelope from the XenForo/Sylphian API.
 * This generic class wraps the actual payload (data) with success status and error information.
 *
 * @param <T> the type of the data payload
 */
public class ApiEnvelope<T> {
    /** Whether the API request was successful from the server's perspective. */
    private boolean success;
    /** The actual data returned by the API. */
    private T data;
    /** A message accompanying the response, often used for errors. */
    private String message;
    /** Detailed validation or logic errors, mapped by field name. */
    private Map<String, List<String>> errors;
    /** Metadata about the response (e.g., pagination, rate limits). */
    private Map<String, Object> meta;

    /**
     * Checks if the API request failed.
     * @return true if success is false, false otherwise
     */
    public boolean isFailed() {
        return !success;
    }

    /**
     * Gets the data payload.
     * @return the data object of type T
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
     * Gets the error details.
     * @return a map of error lists keyed by field
     */
    public Map<String, List<String>> getErrors() {
        return errors;
    }

    /**
     * Gets the response metadata.
     * @return a map of metadata objects
     */
    public Map<String, Object> getMeta() {
        return meta;
    }
}
