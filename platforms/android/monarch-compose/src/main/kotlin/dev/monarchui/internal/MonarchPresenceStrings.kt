package dev.monarchui.internal

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.components.identity.presencedot.MonarchPresenceStatus

/**
 * The spoken and written names of a presence ("Online", "Do not disturb"),
 * for the avatar, the member row, the status picker and every other place
 * that names a presence. The strings are those of `MonarchPresenceDot`.
 */
internal object MonarchPresenceStrings {
    /** The built-in name of [status]. */
    @StringRes
    fun resource(status: MonarchPresenceStatus): Int = when (status) {
        MonarchPresenceStatus.Online -> R.string.monarch_presencedot_online
        MonarchPresenceStatus.Away -> R.string.monarch_presencedot_away
        MonarchPresenceStatus.Busy -> R.string.monarch_presencedot_busy
        MonarchPresenceStatus.DoNotDisturb -> R.string.monarch_presencedot_dnd
        MonarchPresenceStatus.Offline -> R.string.monarch_presencedot_offline
    }
}

/** The name of [status]: [override] when the caller gave one, otherwise the built-in name. */
@Composable
internal fun monarchPresenceName(status: MonarchPresenceStatus, override: String? = null): String =
    override ?: stringResource(MonarchPresenceStrings.resource(status))
