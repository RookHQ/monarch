package com.rookhq.monarch.components.technical.apikeyrow

/** The lifecycle of an API key in a `MonarchApiKeyRow`. Expired and revoked keys have no actions. */
public enum class MonarchApiKeyStatus {
    /** The key works. A success badge. */
    Active,

    /** The key works but expires soon. A warning badge. */
    Expiring,

    /** The key no longer works because it expired. A neutral badge and no actions. */
    Expired,

    /** The key was revoked. A danger badge and no actions. */
    Revoked,
}
