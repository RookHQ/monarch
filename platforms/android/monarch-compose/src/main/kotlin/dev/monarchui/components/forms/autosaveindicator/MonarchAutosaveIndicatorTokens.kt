package dev.monarchui.components.forms.autosaveindicator

import dev.monarchui.components.feedback.savestatusindicator.MonarchSaveStatus

/** The defaults and rules of MonarchAutosaveState and MonarchAutosaveIndicator. */
internal object MonarchAutosaveIndicatorTokens {
    /** Milliseconds to wait after the last change before saving. */
    const val DELAY_MILLIS: Long = 1_000L

    /** Automatic retries after a failed save before the status becomes an error. */
    const val RETRIES: Int = 2

    /** Milliseconds before the first automatic retry; the wait doubles for each further retry. */
    const val RETRY_DELAY_MILLIS: Long = 2_000L

    /** The wait before automatic retry number [attempt] (1 for the first): doubled each time. */
    fun retryWait(retryDelayMillis: Long, attempt: Int): Long =
        retryDelayMillis shl (attempt - 1).coerceIn(0, MAX_SHIFT)

    /** Whether there are changes that are not saved yet: unsaved, saving or failed. */
    fun dirty(status: MonarchSaveStatus): Boolean =
        status == MonarchSaveStatus.Unsaved || status == MonarchSaveStatus.Saving || status == MonarchSaveStatus.Error

    /** The status when nothing is left to save: Saved after a save, Idle before the first. */
    fun settled(savedAt: Long?): MonarchSaveStatus =
        if (savedAt != null) MonarchSaveStatus.Saved else MonarchSaveStatus.Idle

    /** Keeps the doubling from overflowing after many retries. */
    private const val MAX_SHIFT = 20
}
