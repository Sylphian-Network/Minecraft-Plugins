package net.sylphian.velocity.verify.api.model;

import java.util.List;
import java.util.Map;

/**
 * Standard API response envelope used across the Sylphian network.
 * Wraps the primary data payload with status, messages, and error details.
 *
 * @param <T> the type of the contained data
 */
public class ApiEnvelope<T> {
    private boolean success;
    private T data;
    private String message;
    /** Usually populated for validation failures. */
    private Map<String, List<String>> errors;
    private Map<String, Object> meta;

    /** @return true if the request did not succeed */
    public boolean isFailed() {
        return !success;
    }

    /** @return the data payload */
    public T getData() {
        return data;
    }

    /** @return the response message */
    public String getMessage() {
        return message;
    }

    /** @return the error details, keyed by field */
    public Map<String, List<String>> getErrors() {
        return errors;
    }

    /** @return the response metadata */
    public Map<String, Object> getMeta() {
        return meta;
    }
}
