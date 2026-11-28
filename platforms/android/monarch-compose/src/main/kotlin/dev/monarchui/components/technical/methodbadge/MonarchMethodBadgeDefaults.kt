package dev.monarchui.components.technical.methodbadge

import dev.monarchui.foundation.MonarchTone
import java.util.Locale

/** The defaults of `MonarchMethodBadge`. */
public object MonarchMethodBadgeDefaults {
    /**
     * The tone `MonarchMethodBadge` gives [method], ignoring case and white
     * space: reads (GET) are success, creates (POST) info, updates (PUT,
     * PATCH) warning, deletes (DELETE) danger, and every other method,
     * including HEAD and OPTIONS, neutral. The web and iOS use the same map.
     *
     * @param method the HTTP method.
     */
    public fun toneFor(method: String): MonarchTone = when (method.trim().uppercase(Locale.ROOT)) {
        "GET" -> MonarchTone.Success
        "POST" -> MonarchTone.Info
        "PUT", "PATCH" -> MonarchTone.Warning
        "DELETE" -> MonarchTone.Danger
        else -> MonarchTone.Neutral
    }
}
