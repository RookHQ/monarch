package dev.monarchui.sample.showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.charts.barchart.MonarchBarChart
import dev.monarchui.components.charts.barchart.MonarchBarChartSeries
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValue
import dev.monarchui.components.datadisplay.keyvalue.MonarchKeyValueItem
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.files.attachmentchip.MonarchAttachmentChip
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.intelligence.aipromptinput.MonarchAiPromptInput
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueTheme
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/** The box every landing composition is drawn in. */
private val TileWidth = 354.dp
private val TileHeight = 260.dp

/** The width of a hero float's card. */
private val FloatWidth = 240.dp

/** The canvas around a tile or a float in its capture. */
private val CaptureMargin = 24.dp

/** The sheet's grabber in the overlays tile. */
private val GrabberWidth = 36.dp
private val GrabberHeight = 4.dp

/** The size of the attachment in the intelligence tile: 2.4 MB. */
private const val ReportBytes = 2_400_000L

private val ChartMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun")
private val ActiveSeats = listOf(6.0, 7.0, 9.0, 10.0, 11.0, 12.0)

/** The landing page's category tiles, each a small composition of Monarch components. */
enum class LandingTile(val id: String) {
    Actions("actions"),
    Forms("forms"),
    Navigation("navigation"),
    DataDisplay("data-display"),
    Feedback("feedback"),
    Overlays("overlays"),
    Theming("theming"),
    Intelligence("intelligence"),
}

/** The landing page's hero floats: one small card each. */
enum class LandingFloat(val id: String) {
    Toggle("toggle"),
    Progress("progress"),
    Badges("badges"),
}

/**
 * A landing tile as it is captured: its 354 x 260 composition, centred, with 24 of canvas on
 * every side (402 x 308).
 */
@Composable
fun LandingTileCanvas(tile: LandingTile, dark: Boolean, modifier: Modifier = Modifier) {
    ShowcaseTheme(if (dark) CatalogueTheme.Dark else CatalogueTheme.Light) {
        Box(
            modifier = modifier
                .background(MonarchTheme.colors.bgCanvas)
                .padding(CaptureMargin)
                .size(TileWidth, TileHeight),
            contentAlignment = Alignment.Center,
        ) {
            when (tile) {
                LandingTile.Actions -> ActionsTile()
                LandingTile.Forms -> FormsTile()
                LandingTile.Navigation -> NavigationTile()
                LandingTile.DataDisplay -> DataDisplayTile()
                LandingTile.Feedback -> FeedbackTile()
                LandingTile.Overlays -> OverlaysTile()
                LandingTile.Theming -> ThemingTile()
                LandingTile.Intelligence -> IntelligenceTile()
            }
        }
    }
}

/** A hero float as it is captured: its 240-wide card with 24 of canvas on every side. */
@Composable
fun LandingFloatCanvas(float: LandingFloat, dark: Boolean, modifier: Modifier = Modifier) {
    ShowcaseTheme(if (dark) CatalogueTheme.Dark else CatalogueTheme.Light) {
        Box(modifier.background(MonarchTheme.colors.bgCanvas).padding(CaptureMargin)) {
            val padding = if (float == LandingFloat.Toggle) {
                // The switch row brings its own touch-target padding above and below.
                PaddingValues(horizontal = MonarchTheme.spacing.space5, vertical = MonarchTheme.spacing.space1)
            } else {
                PaddingValues(MonarchTheme.spacing.space5)
            }
            MonarchCard(modifier = Modifier.width(FloatWidth), contentPadding = padding) {
                when (float) {
                    LandingFloat.Toggle -> {
                        var on by remember { mutableStateOf(true) }
                        DigestSwitchRow(checked = on, onCheckedChange = { on = it })
                    }
                    LandingFloat.Progress -> RolloutSummary()
                    LandingFloat.Badges -> StatusBadges()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusBadges() {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
        verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
    ) {
        MonarchBadge(text = "Paid", tone = MonarchTone.Success)
        MonarchBadge(text = "New", tone = MonarchTone.Info)
        MonarchBadge(text = "Reverted", tone = MonarchTone.Warning)
    }
}

@Composable
private fun ActionsTile() {
    val spacing = MonarchTheme.spacing
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
            MonarchButton(text = "Invite people", onClick = {}, variant = MonarchButtonVariant.Primary)
            MonarchButton(text = "View rollout", onClick = {}, variant = MonarchButtonVariant.Secondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
            MonarchButton(text = "Cancel", onClick = {}, variant = MonarchButtonVariant.Ghost)
            MonarchButton(text = "Remove", onClick = {}, variant = MonarchButtonVariant.Danger)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
            MonarchButton(text = "Saving", onClick = {}, variant = MonarchButtonVariant.Secondary, loading = true)
            MonarchButton(text = "Archive", onClick = {}, variant = MonarchButtonVariant.Secondary, enabled = false)
        }
    }
}

@Composable
private fun FormsTile() {
    var name by remember { mutableStateOf("Acme Studio") }
    var email by remember { mutableStateOf("billing@acme") }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
    ) {
        MonarchTextField(
            value = name,
            onValueChange = { name = it },
            label = "Workspace name",
            modifier = Modifier.fillMaxWidth(),
        )
        MonarchTextField(
            value = email,
            onValueChange = { email = it },
            label = "Billing email",
            errorText = "Enter a complete email address.",
            modifier = Modifier.fillMaxWidth(),
        )
        MonarchTextField(
            value = "ws_4f2a",
            onValueChange = {},
            label = "Workspace ID",
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun NavigationTile() {
    var selected by remember { mutableIntStateOf(0) }
    Column(
        verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MonarchSegmentedControl(
            options = listOf("Overview", "Activity", "Settings"),
            selectedIndex = selected,
            onSelect = { selected = it },
        )
        MonarchKeyValue(
            items = listOf(
                MonarchKeyValueItem("Plan", "Business"),
                MonarchKeyValueItem("Seats", "12 of 20"),
                MonarchKeyValueItem("Region", "Frankfurt"),
            ),
        )
    }
}

@Composable
private fun DataDisplayTile() {
    MonarchBarChart(
        title = "Active seats",
        description = "Last 6 months",
        categories = ChartMonths,
        series = listOf(MonarchBarChartSeries("seats", "Seats", ActiveSeats)),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FeedbackTile() {
    Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
        MonarchBanner(
            title = "Release 2.4 is rolling out",
            message = "3 of 5 regions are done.",
            tone = MonarchTone.Info,
        )
        MonarchBanner(
            title = "2 seats left",
            message = "Add seats before you invite more people.",
            tone = MonarchTone.Warning,
        )
        MonarchBanner(
            title = "Invoice 1042 paid",
            message = "A receipt went to billing@acme.com.",
            tone = MonarchTone.Success,
        )
    }
}

@Composable
private fun OverlaysTile() {
    val colors = MonarchTheme.colors
    val spacing = MonarchTheme.spacing
    // A small screen: its corners round like the sheet's.
    Box(Modifier.fillMaxSize().clip(MonarchTheme.shapes.xl)) {
        // The screen behind the sheet: the top of the Recent activity card.
        RecentActivityCard(items = RecentActivity.take(2))
        Box(Modifier.fillMaxSize().background(colors.overlayScrim))
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner))
                .background(colors.bgRaised)
                .padding(start = spacing.space4, end = spacing.space4, bottom = spacing.space4),
            verticalArrangement = Arrangement.spacedBy(spacing.space2),
        ) {
            Box(
                Modifier
                    .padding(top = spacing.space2, bottom = spacing.space2)
                    .align(Alignment.CenterHorizontally)
                    .size(GrabberWidth, GrabberHeight)
                    .clip(MonarchTheme.shapes.full)
                    .background(colors.borderDefault),
            )
            MonarchText(text = "Remove Dana Novak?", style = MonarchTextStyle.Subheading)
            MonarchText(text = "They lose access to Acme Studio.", color = colors.textMuted)
            Row(
                modifier = Modifier.padding(top = spacing.space2),
                horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            ) {
                MonarchButton(
                    text = "Cancel",
                    onClick = {},
                    variant = MonarchButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                MonarchButton(
                    text = "Remove",
                    onClick = {},
                    variant = MonarchButtonVariant.Danger,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** The sheet's top corners: the xl radius token. */
private val SheetCorner = MonarchTokens.Radius.xl.dp

@Composable
private fun ThemingTile() {
    Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
        ThemedRow(CatalogueTheme.Light, "Light")
        ThemedRow(CatalogueTheme.Dark, "Dark")
        ThemedRow(CatalogueTheme.HighContrastLight, "High contrast")
    }
}

/** A small card in its own theme, so the three themes sit side by side. */
@Composable
private fun ThemedRow(theme: CatalogueTheme, label: String) {
    ShowcaseTheme(theme) {
        // The small button's touch target adds 8 above and below it, so the card pads 8 vertically.
        MonarchCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = MonarchTheme.spacing.space4,
                vertical = MonarchTheme.spacing.space2,
            ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            ) {
                MonarchText(text = label, color = MonarchTheme.colors.textDefault)
                Spacer(Modifier.weight(1f))
                MonarchBadge(text = "Active", tone = MonarchTone.Success)
                MonarchButton(
                    text = "Invite",
                    onClick = {},
                    variant = MonarchButtonVariant.Primary,
                    size = MonarchButtonSize.Sm,
                )
            }
        }
    }
}

@Composable
private fun IntelligenceTile() {
    val prompt = rememberTextFieldState("Summarise the Q3 report for the team")
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
    ) {
        MonarchAttachmentChip(name = "Q3 report.pdf", sizeBytes = ReportBytes)
        MonarchAiPromptInput(
            label = "Message the assistant",
            state = prompt,
            onSubmit = {},
            busy = false,
            keyboardPadding = false,
            sendIcon = { MonarchIcon(SampleIcons.Send, contentDescription = null, size = MonarchIconSize.Sm) },
        )
    }
}
