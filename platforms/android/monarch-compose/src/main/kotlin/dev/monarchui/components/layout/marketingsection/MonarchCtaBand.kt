package dev.monarchui.components.layout.marketingsection

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme

/**
 * The closing call to action of a marketing screen: a centred title, an optional description and the actions, on
 * the subtle accent fill. Use it once, at the end.
 *
 * @param title the title, read as a heading.
 * @param modifier the modifier of the band.
 * @param description a muted line under the title.
 * @param actions the buttons, normally one primary `MonarchButton`; they wrap onto more lines when needed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchCtaBand(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actions: (@Composable FlowRowScope.() -> Unit)? = null,
) {
    val colors = MonarchTheme.colors
    val shape = MonarchShapeDefaults.Container
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
            .background(MonarchMarketingSectionTokens.ctaFill(colors), shape)
            .border(MonarchShapeDefaults.BorderWidth, MonarchMarketingSectionTokens.ctaBorder(colors), shape)
            .padding(MonarchMarketingSectionTokens.SectionPadding),
        verticalArrangement = Arrangement.spacedBy(MonarchMarketingSectionTokens.GridGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MonarchText(text = title, style = MonarchTextStyle.Heading, textAlign = TextAlign.Center, heading = true)
        if (description != null) {
            MonarchText(text = description, color = colors.textMuted, textAlign = TextAlign.Center)
        }
        if (actions != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(
                    MonarchMarketingSectionTokens.CardGap,
                    Alignment.CenterHorizontally,
                ),
                verticalArrangement = Arrangement.spacedBy(MonarchMarketingSectionTokens.CardGap),
                content = actions,
            )
        }
    }
}
