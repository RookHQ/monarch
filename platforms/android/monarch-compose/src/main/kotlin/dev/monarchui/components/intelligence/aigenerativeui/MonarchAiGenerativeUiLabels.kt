package dev.monarchui.components.intelligence.aigenerativeui

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of `MonarchAiGenerativeUi`. Every
 * `null` keeps the built-in string `monarch_aigenerativeui_<key>`.
 *
 * @property unknown shown instead of a component that is not in the registry: "This part of the answer can't
 * be shown here."
 * @property invalid shown when the props are not valid: "This part of the answer had data that couldn't be
 * shown."
 * @property loading the TalkBack name of the progress indicator while props stream, "Loading".
 */
@Immutable
public data class MonarchAiGenerativeUiLabels(
    public val unknown: String? = null,
    public val invalid: String? = null,
    public val loading: String? = null,
)
