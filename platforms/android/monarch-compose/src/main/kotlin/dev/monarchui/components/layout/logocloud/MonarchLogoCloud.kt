package dev.monarchui.components.layout.logocloud

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.monarchui.R
import dev.monarchui.accessibility.monarchMinimumTouchTarget
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.MonarchSmallCapsLabel
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A titled row or grid of organisation logos, such as customers or integrations on a welcome screen, with the name
 * as a wordmark while a logo has no image.
 *
 * Built from Compose `FlowRow` and `Image`. Logos are quiet: images are drawn in the muted text colour when
 * [monochrome] is on, and wordmarks always are. A logo with a link is a button with a 48dp touch target that calls
 * [onLogoClick]; the others are read as text. The group is a traversal group named by [title].
 *
 * @param logos the organisations, in reading order.
 * @param modifier the modifier of the cloud.
 * @param title the small heading above the logos, such as "Placeholder organisations".
 * @param layout a centred wrapping row or equal bordered cells.
 * @param size the height of the logos.
 * @param monochrome draws logo images in one quiet colour.
 * @param onLogoClick called with a logo that has a link when the user taps it.
 * @param listLabel the spoken name of the group without a title. `null` uses "Organisations".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchLogoCloud(
    logos: List<MonarchLogoCloudLogo>,
    modifier: Modifier = Modifier,
    title: String? = null,
    layout: MonarchLogoCloudLayout = MonarchLogoCloudLayout.Row,
    size: MonarchLogoCloudSize = MonarchLogoCloudSize.Md,
    monochrome: Boolean = true,
    onLogoClick: ((MonarchLogoCloudLogo) -> Unit)? = null,
    listLabel: String? = null,
) {
    val groupName = title ?: monarchString(listLabel, R.string.monarch_logocloud_list_label)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                isTraversalGroup = true
                paneTitle = groupName
            },
        verticalArrangement = Arrangement.spacedBy(MonarchLogoCloudTokens.TitleGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (title != null) MonarchSmallCapsLabel(text = title)
        when (layout) {
            MonarchLogoCloudLayout.Row -> FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    MonarchLogoCloudTokens.RowGapHorizontal,
                    Alignment.CenterHorizontally,
                ),
                verticalArrangement = Arrangement.spacedBy(MonarchLogoCloudTokens.RowGapVertical),
            ) {
                logos.forEach { logo ->
                    LogoCell(logo, size, monochrome, onLogoClick, Modifier.align(Alignment.CenterVertically))
                }
            }
            MonarchLogoCloudLayout.Grid -> LogoGrid(logos, size, monochrome, onLogoClick)
        }
    }
}

@Composable
private fun LogoGrid(
    logos: List<MonarchLogoCloudLogo>,
    size: MonarchLogoCloudSize,
    monochrome: Boolean,
    onLogoClick: ((MonarchLogoCloudLogo) -> Unit)?,
) {
    val gap = MonarchLogoCloudTokens.CellGap
    val colors = MonarchTheme.colors
    val shape = MonarchShapeDefaults.Container
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val perRow = ((maxWidth + gap) / (MonarchLogoCloudTokens.MinCellWidth + gap)).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            logos.chunked(perRow).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { logo ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(shape)
                                .border(
                                    MonarchShapeDefaults.BorderWidth,
                                    MonarchLogoCloudTokens.cellBorder(colors),
                                    shape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            LogoCell(
                                logo,
                                size,
                                monochrome,
                                onLogoClick,
                                Modifier.fillMaxWidth().padding(MonarchLogoCloudTokens.CellPadding),
                            )
                        }
                    }
                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun LogoCell(
    logo: MonarchLogoCloudLogo,
    size: MonarchLogoCloudSize,
    monochrome: Boolean,
    onLogoClick: ((MonarchLogoCloudLogo) -> Unit)?,
    modifier: Modifier,
) {
    val colors = MonarchTheme.colors
    val linked = logo.link != null && onLogoClick != null
    val interaction = if (linked) {
        Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchShapeDefaults.Control)
            .clickable(role = Role.Button) { onLogoClick(logo) }
    } else {
        Modifier
    }
    Box(modifier.then(interaction), contentAlignment = Alignment.Center) {
        val painter = if (MonarchTheme.isDark) logo.imageDark ?: logo.image else logo.image
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = logo.name,
                modifier = Modifier.height(MonarchLogoCloudTokens.height(size)),
                contentScale = ContentScale.FillHeight,
                colorFilter = if (monochrome) ColorFilter.tint(MonarchLogoCloudTokens.wordmark(colors)) else null,
            )
        } else {
            MonarchText(
                text = logo.name,
                style = MonarchLogoCloudTokens.wordmarkStyle(size),
                color = MonarchLogoCloudTokens.wordmark(colors),
                textAlign = TextAlign.Center,
                heading = false,
            )
        }
    }
}
