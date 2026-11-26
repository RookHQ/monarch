package com.rookhq.monarch.components.forms.switchsettingslist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * A group of on/off settings that each save on their own, with a saving,
 * saved and failed state and a retry on each row.
 *
 * Built by Monarch: a heading, a description and one hairline card that holds
 * the `MonarchSwitchSettingsRow`s (`MonarchSwitch` rows) with a subtle divider
 * between them, as on the web. The list
 * gives its rows the [labels] and the [savedDurationMillis]; a row can
 * replace them. Use it where a change takes effect at once and must reach a
 * server, such as notification settings.
 *
 * @param modifier the modifier of the list.
 * @param title the heading of the group, for example "Email notifications". A heading for TalkBack.
 * @param description a sentence under the heading.
 * @param labels replacements for the built-in strings of every row.
 * @param savedDurationMillis how long "Saved" stays after a save.
 * @param content the rows, usually `MonarchSwitchSettingsRow`s.
 */
@Composable
public fun MonarchSwitchSettingsList(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    labels: MonarchSwitchSettingsListLabels = MonarchSwitchSettingsListLabels(),
    savedDurationMillis: Long = MonarchSwitchSettingsListDefaults.SavedDurationMillis,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MonarchSwitchSettingsListTokens
    val card = remember { SettingsCardGeometry() }
    val config = remember(labels, savedDurationMillis) {
        MonarchSwitchSettingsListConfig(labels, savedDurationMillis, card)
    }
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    val shape = MonarchTheme.shapes.lg
    Column(modifier = modifier) {
        if (title != null || description != null) {
            Column(
                modifier = Modifier.padding(bottom = tokens.HeaderBottomPadding),
                verticalArrangement = Arrangement.spacedBy(tokens.HeaderGap),
            ) {
                // The web's header: 14sp semibold title, 14sp muted description.
                if (title != null) {
                    Text(
                        text = title,
                        modifier = Modifier.semantics { heading() },
                        style = typography.label.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textDefault,
                    )
                }
                if (description != null) {
                    Text(
                        text = description,
                        style = typography.label.copy(fontWeight = FontWeight.Normal),
                        color = tokens.descriptionColor(colors),
                    )
                }
            }
        }
        // The rows sit in one hairline card with a subtle divider between them, as on the web.
        val divider = colors.borderSubtle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onPlaced { card.originY = it.positionInRoot().y }
                .background(colors.bgSurface, shape)
                .border(MonarchTokens.Space.px.dp, colors.borderDefault, shape)
                .drawWithContent {
                    drawContent()
                    val hairline = MonarchTokens.Space.px.dp.toPx()
                    card.dividers().forEach { y ->
                        drawLine(divider, Offset(0f, y), Offset(size.width, y), hairline)
                    }
                },
        ) {
            CompositionLocalProvider(LocalMonarchSwitchSettingsList provides config) { content() }
        }
    }
}
