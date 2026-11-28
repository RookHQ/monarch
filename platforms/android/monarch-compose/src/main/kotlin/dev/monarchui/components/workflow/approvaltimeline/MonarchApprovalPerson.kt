package dev.monarchui.components.workflow.approvaltimeline

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter

/**
 * A person in an approval: who asked, who decided or who can decide. Used by `MonarchApprovalTimeline`.
 *
 * @property name the display name, already localised.
 * @property image the picture, loaded by the app. Initials while it is `null`.
 * @property seed picks the colour of the initials, so a renamed person keeps their colour. The name by default.
 */
@Immutable
public class MonarchApprovalPerson(
    public val name: String,
    public val image: Painter? = null,
    public val seed: String? = null,
)
