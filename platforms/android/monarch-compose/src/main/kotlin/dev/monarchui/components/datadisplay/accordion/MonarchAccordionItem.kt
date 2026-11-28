package dev.monarchui.components.datadisplay.accordion

import androidx.compose.runtime.Immutable

/**
 * One section of a `MonarchAccordion`.
 *
 * @property id the stable id of the section; the value kept in the expanded set.
 * @property title the header text, read as a heading. Already localised.
 * @property enabled whether the user can open and close this section.
 * @property subtitle a second line under the title, in the muted style, such as a summary of the content
 * ("3 rules, 1 off"). Read after the title.
 */
@Immutable
public class MonarchAccordionItem(
    public val id: String,
    public val title: String,
    public val enabled: Boolean = true,
    public val subtitle: String? = null,
)
