package net.sylphian.verify.paper.api.model;

public enum VerificationReason {
    UUID_NOT_LINKED,
    ACCOUNT_NOT_CONFIRMED,
    BRUTE_FORCE_BLOCKED,
    RE_VERIFICATION_FAILED,
    API_ERROR,
    API_SUCCESS_NO_DATA,
    API_FAILURE_NO_MESSAGE
}
