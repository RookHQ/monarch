package dev.monarchui.components.datadisplay.insightstrip

import androidx.compose.runtime.Immutable
import dev.monarchui.foundation.MonarchTone

/**
 * One insight in a `MonarchInsightStrip`.
 *
 * @property id a stable key, passed to `onDismiss`.
 * @property title the insight in one line.
 * @property why why it was raised, in plain words.
 * @property origin whether a rule or a model raised it. Always shown.
 * @property tone how much it matters: `Danger` for critical, `Warning`, `Info`, `Success` for positive,
 * `Neutral` for a note. Shown as a word as well as a colour.
 * @property evidence the figures or records behind it, shown in a collapsed section.
 * @property actionLabel the label of one follow-up action, for example "Review renewals".
 * @property onAction called when the action is activated. The button is shown only with [actionLabel].
 */
@Immutable
public class MonarchInsight(
    public val id: String,
    public val title: String,
    public val why: String,
    public val origin: MonarchInsightOrigin,
    public val tone: MonarchTone = MonarchTone.Info,
    public val evidence: String? = null,
    public val actionLabel: String? = null,
    public val onAction: (() -> Unit)? = null,
)
