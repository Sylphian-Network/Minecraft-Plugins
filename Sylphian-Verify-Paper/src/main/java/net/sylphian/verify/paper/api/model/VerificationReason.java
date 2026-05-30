package net.sylphian.verify.paper.api.model;

/**
 * Enumeration of reasons why a player verification might fail or be restricted.
 * Used to provide specific feedback to the player and logging systems.
 */
public enum VerificationReason {
    /** The player's Mojang UUID is not linked to any forum account. */
    UUID_NOT_LINKED,
    
    /** The forum account is linked but hasn't been email-confirmed yet. */
    ACCOUNT_NOT_CONFIRMED,
    
    /** Too many failed attempts detected; the account is temporarily blocked. */
    BRUTE_FORCE_BLOCKED,
    
    /** A periodic check failed (e.g., account was unlinked or banned on forum). */
    RE_VERIFICATION_FAILED,
    
    /** A technical error occurred while communicating with the API. */
    API_ERROR,
    
    /** API call returned 200 OK but the data payload was missing. */
    API_SUCCESS_NO_DATA,
    
    /** API call returned a failure status but provided no specific error message. */
    API_FAILURE_NO_MESSAGE
}
