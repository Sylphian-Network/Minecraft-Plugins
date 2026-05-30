package net.sylphian.verify.velocity.api.model;

/**
 * Reasons for verification failure or access denial on the Velocity proxy.
 */
public enum VerificationReason {
    /** The player's Mojang UUID is not linked to any forum account. */
    UUID_NOT_LINKED,
    
    /** The forum account is linked but not confirmed (e.g., email not verified). */
    ACCOUNT_NOT_CONFIRMED,
    
    /** Access blocked due to too many failed attempts from this identity or IP. */
    BRUTE_FORCE_BLOCKED,
    
    /** A periodic check failed, indicating the link is no longer valid. */
    RE_VERIFICATION_FAILED,
    
    /** An internal technical error occurred while contacting the verification API. */
    API_ERROR,
    
    /** The API returned success but without the expected player data. */
    API_SUCCESS_NO_DATA,
    
    /** The API reported a failure but provided no specific error message. */
    API_FAILURE_NO_MESSAGE
}
