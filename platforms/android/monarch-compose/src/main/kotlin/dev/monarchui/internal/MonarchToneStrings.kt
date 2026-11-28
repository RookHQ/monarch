package dev.monarchui.internal

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.foundation.MonarchTone

/**
 * The spoken names of the tones, read by TalkBack before the content of a
 * toned component ("Warning, Storage almost full"). One table for every
 * component, the same words as iOS.
 */
internal object MonarchToneStrings {
    /**
     * The built-in name of [tone], or `null` for the neutral and accent tones,
     * which carry emphasis but no meaning to announce.
     */
    @StringRes
    fun resource(tone: MonarchTone): Int? = when (tone) {
        MonarchTone.Neutral, MonarchTone.Accent -> null
        MonarchTone.Info -> R.string.monarch_internal_tone_info
        MonarchTone.Success -> R.string.monarch_internal_tone_success
        MonarchTone.Warning -> R.string.monarch_internal_tone_warning
        MonarchTone.Danger -> R.string.monarch_internal_tone_danger
        MonarchTone.Ai -> R.string.monarch_internal_tone_ai
    }
}

/**
 * The spoken name of [tone]: [override] when the caller gave one, otherwise
 * the built-in name, or `null` for a tone that has none.
 */
@Composable
internal fun monarchToneName(tone: MonarchTone, override: String? = null): String? =
    override ?: MonarchToneStrings.resource(tone)?.let { stringResource(it) }

/**
 * [monarchToneName] for a component that still ships its own tone-name
 * string keys (`monarch_<component>_tone_*`), deprecated per D7.
 *
 * The default always comes from the shared table ([MonarchToneStrings]). The
 * component's own key is read only to keep an app's resource override
 * working: its default value is an alias of the shared string, so it counts as
 * an override only when an app redefined it to a different text.
 *
 * @param legacy the component's deprecated key for a tone, or `null` when it has none.
 */
@Composable
internal fun monarchToneName(
    tone: MonarchTone,
    override: String?,
    legacy: (MonarchTone) -> Int?,
): String? {
    if (override != null) return override
    val shared = MonarchToneStrings.resource(tone)?.let { stringResource(it) }
    val appOverride = legacy(tone)?.let { stringResource(it) }?.takeIf { it != shared }
    return monarchToneName(tone, appOverride)
}
