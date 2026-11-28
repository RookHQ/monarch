package dev.monarchui.components.feedback.errorstate

import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors

/** Component tokens of MonarchErrorState, built from roles only. Sizes are those of MonarchEmptyState. */
internal object MonarchErrorStateTokens {
    /** Whether a retry can change the outcome of [kind]. */
    fun offersRetry(kind: MonarchErrorStateKind): Boolean =
        kind != MonarchErrorStateKind.Forbidden && kind != MonarchErrorStateKind.NotFound

    /**
     * The icon tile: the danger tint for failures, the warning tint when
     * offline, and the muted background for states that are not failures.
     */
    fun tileColors(kind: MonarchErrorStateKind, colors: MonarchColors): MonarchSurfaceColors = when (kind) {
        MonarchErrorStateKind.Error, MonarchErrorStateKind.Server ->
            MonarchSurfaceColors(colors.dangerSubtle, colors.dangerStrong, null)
        MonarchErrorStateKind.Offline -> MonarchSurfaceColors(colors.warningSubtle, colors.warningStrong, null)
        MonarchErrorStateKind.Forbidden, MonarchErrorStateKind.NotFound, MonarchErrorStateKind.Maintenance ->
            MonarchSurfaceColors(colors.bgMuted, colors.textMuted, null)
    }
}
