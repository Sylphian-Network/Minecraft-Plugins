package net.sylphian.velocity.verify.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Data model for the verification payload returned by the API.
 * Contains decision logic (allowed status) and linked account details.
 */
public class VerificationResponse {
    @SerializedName("allowed")
    private boolean allowed;

    private VerificationReason reason;

    /** Only populated if the account is not yet linked. */
    private String passcode;

    @SerializedName("forum_user_id")
    private int xfUserId;

    @SerializedName("forum_username")
    private String forumUsername;

    @SerializedName("minecraft_username")
    private String mcUsername;

    @SerializedName("confirmed_date")
    private Long confirmedDate;

    /** Used by Gson for deserialization. */
    public VerificationResponse() {
    }

    /**
     * @param allowed whether access is granted
     * @param reason  the reason for the status
     */
    public VerificationResponse(boolean allowed, VerificationReason reason) {
        this.allowed = allowed;
        this.reason = reason;
    }

    /** @return true if the player is allowed to connect */
    public boolean isAllowed() {
        return allowed;
    }

    /** @return the reason for the current status */
    public VerificationReason getReason() {
        return reason;
    }

    /** @return the link passcode, or null if the account is already linked */
    public String getPasscode() {
        return passcode;
    }

    /** @return the linked forum user's ID */
    public int getXfUserId() {
        return xfUserId;
    }

    /** @return the linked forum username */
    public String getForumUsername() {
        return forumUsername;
    }

    /** @return the linked Minecraft username */
    public String getMcUsername() {
        return mcUsername;
    }

    /** @return true if the linked account has a confirmation timestamp */
    public boolean isConfirmed() {
        return confirmedDate != null;
    }
}
