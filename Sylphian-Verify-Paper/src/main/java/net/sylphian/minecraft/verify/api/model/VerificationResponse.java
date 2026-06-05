package net.sylphian.minecraft.verify.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the detailed verification data returned by the API for a specific player.
 * Contains both the access status (allowed or not) and the associated forum identity details.
 */
public class VerificationResponse {
    /** Whether the player is permitted to join based on their verification status. */
    @SerializedName("allowed")
    private boolean allowed;

    /** The reason for rejection if 'allowed' is false. */
    private VerificationReason reason;
    
    /** A temporary passcode for linking, if applicable. */
    private String passcode;

    /** The unique ID of the linked user on the forum. */
    @SerializedName("forum_user_id")
    private int xfUserId;

    /** The username of the linked account on the forum. */
    @SerializedName("forum_username")
    private String forumUsername;

    /** The Minecraft username currently recorded for this link. */
    @SerializedName("minecraft_username")
    private String mcUsername;

    /** The epoch timestamp when the link was confirmed, or null if unconfirmed. */
    @SerializedName("confirmed_date")
    private Long confirmedDate;

    /**
     * Default constructor for Gson deserialization.
     */
    public VerificationResponse() {
    }

    /**
     * Constructs a response with a status and reason.
     *
     * @param allowed whether access is granted
     * @param reason  the reason for the status
     */
    public VerificationResponse(boolean allowed, VerificationReason reason) {
        this.allowed = allowed;
        this.reason = reason;
    }

    /**
     * Checks if the player is allowed to join.
     * @return true if allowed, false otherwise
     */
    public boolean isAllowed() {
        return allowed;
    }

    /**
     * Gets the reason for the verification outcome.
     * @return the verification reason
     */
    public VerificationReason getReason() {
        return reason;
    }

    /**
     * Gets the link passcode if provided by the API.
     * @return the passcode string
     */
    public String getPasscode() {
        return passcode;
    }

    /**
     * Gets the XenForo user ID.
     * @return the forum user ID
     */
    public int getXfUserId() {
        return xfUserId;
    }

    /**
     * Gets the forum username.
     * @return the forum username
     */
    public String getForumUsername() {
        return forumUsername;
    }

    /**
     * Gets the Minecraft username.
     * @return the Minecraft username
     */
    public String getMcUsername() {
        return mcUsername;
    }

    /**
     * Checks if the account link is confirmed.
     * @return true if confirmed date is not null
     */
    public boolean isConfirmed() {
        return confirmedDate != null;
    }
}
