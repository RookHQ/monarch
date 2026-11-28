package dev.monarchui.foundation.icon

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.tokens.MonarchTokens

/** The size of a `MonarchIcon`. */
public enum class MonarchIconSize {
    /** 16dp. Inside small buttons, badges and captions. */
    Sm,

    /** 20dp. Inside buttons, fields and banners. The default. */
    Md,

    /** 24dp. On its own, for example in a toolbar. */
    Lg,
}

/** Component tokens of MonarchIcon, built from primitives only. */
internal val MonarchIconSize.dp: Dp
    get() = when (this) {
        MonarchIconSize.Sm -> MonarchTokens.Space.space4.dp
        MonarchIconSize.Md -> MonarchTokens.Space.space5.dp
        MonarchIconSize.Lg -> MonarchTokens.Space.space6.dp
    }
