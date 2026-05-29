package net.sylphian.verify.velocity.api.model;

import com.google.gson.annotations.SerializedName;

public class VerificationResponse {
    @SerializedName("allowed")
    private boolean allowed;

    private VerificationReason reason;
    private String passcode;

    @SerializedName("forum_user_id")
    private int xfUserId;

    @SerializedName("forum_username")
    private String forumUsername;

    @SerializedName("minecraft_username")
    private String mcUsername;

    @SerializedName("confirmed_date")
    private Long confirmedDate;

    public VerificationResponse() {
    }

    public VerificationResponse(boolean allowed, VerificationReason reason) {
        this.allowed = allowed;
        this.reason = reason;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public VerificationReason getReason() {
        return reason;
    }

    public String getPasscode() {
        return passcode;
    }

    public int getXfUserId() {
        return xfUserId;
    }

    public String getForumUsername() {
        return forumUsername;
    }

    public String getMcUsername() {
        return mcUsername;
    }

    public boolean isConfirmed() {
        return confirmedDate != null;
    }
}
