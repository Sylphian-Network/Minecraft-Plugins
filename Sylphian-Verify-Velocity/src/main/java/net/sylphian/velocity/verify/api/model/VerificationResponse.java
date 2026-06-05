package net.sylphian.velocity.verify.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data model for the verification payload returned by the API.
 * Contains decision logic (allowed status) and linked account details.
 */
public class VerificationResponse {
    /** Whether the player is allowed to connect. */
    @SerializedName("allowed")
    private boolean allowed;

    /** The reason for the current status. */
    private VerificationReason reason;
    
    /** A temporary link code, if the account is not yet linked. */
    private String passcode;

    /** The unique ID of the linked forum user. */
    @SerializedName("forum_user_id")
    private int xfUserId;

    /** The username of the linked forum user. */
    @SerializedName("forum_username")
    private String forumUsername;

    /** The Minecraft username linked to the forum account. */
    @SerializedName("minecraft_username")
    private String mcUsername;

    /** The timestamp when the link was confirmed, if applicable. */
    @SerializedName("confirmed_date")
    private Long confirmedDate;

    /**
     * Default constructor for JSON deserialization.
     */
    public VerificationResponse() {
    }

    /**
     * Constructs a response with an explicit status and reason.
     *
     * @param allowed whether access is granted
     * @param reason  the reason for the status
     */
    public VerificationResponse(boolean allowed, VerificationReason reason) {
        this.allowed = allowed;
        this.reason = reason;
    }

    /**
     * Gets the access status.
     * @return true if allowed, false otherwise
     */
    public boolean isAllowed() {
        return allowed;
    }

    /**
     * Gets the verification reason.
     * @return the reason enum
     */
    public VerificationReason getReason() {
        return reason;
    }

    /**
     * Gets the link passcode.
     * @return the passcode string
     */
    public String getPasscode() {
        return passcode;
    }

    /**
     * Gets the forum user ID.
     * @return the ID
     */
    public int getXfUserId() {
        return xfUserId;
    }

    /**
     * Gets the forum username.
     * @return the username
     */
    public String getForumUsername() {
        return forumUsername;
    }

    /**
     * Gets the linked Minecraft username.
     * @return the username
     */
    public String getMcUsername() {
        return mcUsername;
    }

    /**
     * Checks if the link is confirmed.
     * @return true if confirmedDate is not null
     */
    public boolean isConfirmed() {
        return confirmedDate != null;
    }
}
