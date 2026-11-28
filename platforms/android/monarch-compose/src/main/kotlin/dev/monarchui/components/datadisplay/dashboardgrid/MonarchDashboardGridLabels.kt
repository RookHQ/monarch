package dev.monarchui.components.datadisplay.dashboardgrid

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchDashboardGrid`. Every
 * `null` uses the built-in string named after it in `strings_dashboardgrid.xml`.
 * The functions receive the widget's label and, for the announcements, its
 * position from one and the number of widgets.
 *
 * @property label the spoken name of the grid: "Dashboard".
 * @property handle the spoken name of a widget's drag handle in edit mode: "Move Team load".
 * @property earlier the visible text of the Move earlier button: "Earlier".
 * @property later the visible text of the Move later button: "Later".
 * @property moveEarlier the spoken name of the Move earlier button: "Move Team load earlier".
 * @property moveLater the spoken name of the Move later button: "Move Team load later".
 * @property pickedUp announced when a drag starts: "Picked up Team load. Position 2 of 6.".
 * @property moved announced after a move by a button or an action: "Team load moved to position 3 of 6.".
 * @property dropped announced when a drag ends: "Team load dropped at position 3 of 6.".
 * @property cancelled announced when a drag is cancelled: "Move cancelled. Team load returned to position 2
 * of 6.".
 */
@Immutable
public class MonarchDashboardGridLabels(
    public val label: String? = null,
    public val handle: ((label: String) -> String)? = null,
    public val earlier: String? = null,
    public val later: String? = null,
    public val moveEarlier: ((label: String) -> String)? = null,
    public val moveLater: ((label: String) -> String)? = null,
    public val pickedUp: ((label: String, position: Int, total: Int) -> String)? = null,
    public val moved: ((label: String, position: Int, total: Int) -> String)? = null,
    public val dropped: ((label: String, position: Int, total: Int) -> String)? = null,
    public val cancelled: ((label: String, position: Int, total: Int) -> String)? = null,
)
