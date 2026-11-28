package dev.monarchui.sample.showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.datadisplay.listrow.MonarchListRow
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.forms.select.MonarchSelect
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarGroup
import dev.monarchui.components.identity.avatar.MonarchAvatarShape
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.fonts.poppins.monarchPoppinsTypography
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueTheme
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchThemeTokens
import dev.monarchui.tokens.MonarchTokens

/** From this width on, the showcase puts a sidebar beside the content instead of a bottom bar. */
private val SidebarBreakpoint = 600.dp

/** The widest the content column gets on a tablet. */
private val ShowcaseContentMaxWidth = 640.dp

/** The width of the tablet sidebar. */
private val SidebarWidth = 280.dp

/** The tablet content keeps clear of the system bars; the sidebar takes the start edge. */
private val ContentInsets: WindowInsets
    @Composable get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)

/** How far the release has rolled out. */
private const val RolloutProgress = 0.6f

/** A destination of the showcase's navigation. */
private class ShowcaseDestination(val label: String, val icon: ImageVector)

private val Destinations = listOf(
    ShowcaseDestination("Overview", SampleIcons.Home),
    ShowcaseDestination("Activity", SampleIcons.Bell),
    ShowcaseDestination("Settings", SampleIcons.Settings),
)

/** The people on the release team, in the order the avatar group shows them. */
internal val ReleaseTeam = listOf("Ana Silva", "Ben Okafor", "Chen Wei", "Dana Novak", "Eli Park")

/** The two regions the workspace can be stored in. */
private val Regions = listOf("Europe (Frankfurt)", "United States (Virginia)")

/** One row of the Recent activity card. */
internal class ActivityItem(val title: String, val detail: String, val status: String, val tone: MonarchTone)

internal val RecentActivity = listOf(
    ActivityItem("Invoice 1042 paid", "Ana Silva · 2 h ago", "Paid", MonarchTone.Success),
    ActivityItem("Chen Wei joined", "Invited by Ben Okafor · Yesterday", "New", MonarchTone.Info),
    ActivityItem("Release 2.3 rolled back", "Dana Novak · Monday", "Reverted", MonarchTone.Warning),
)

/** The theme tokens of the showcase: light or dark, with the default Rook accent. */
internal fun showcaseTokens(theme: CatalogueTheme): MonarchThemeTokens =
    theme.tokens.withAccent(MonarchTokens.Accents.rook)

/** The showcase's theme: the given tokens in Poppins. */
@Composable
internal fun ShowcaseTheme(theme: CatalogueTheme, content: @Composable () -> Unit) {
    MonarchTheme(tokens = showcaseTokens(theme), typography = monarchPoppinsTypography(), content = content)
}

/**
 * The landing page's showcase: the Overview screen of the workspace Acme Studio, built from
 * Monarch components. A bottom navigation bar on a phone; a sidebar beside the content from
 * 600 dp on.
 *
 * @param dark whether to show the dark theme instead of the light one.
 */
@Composable
fun ShowcaseApp(dark: Boolean) {
    ShowcaseTheme(if (dark) CatalogueTheme.Dark else CatalogueTheme.Light) {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        BoxWithConstraints(Modifier.fillMaxSize().background(MonarchTheme.colors.bgCanvas)) {
            if (maxWidth >= SidebarBreakpoint) {
                TabletShowcase(selected) { selected = it }
            } else {
                PhoneShowcase(selected) { selected = it }
            }
        }
    }
}

@Composable
private fun PhoneShowcase(selected: Int, onSelect: (Int) -> Unit) {
    Scaffold(
        containerColor = MonarchTheme.colors.bgCanvas,
        contentColor = MonarchTheme.colors.textDefault,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(containerColor = MonarchTheme.colors.bgSurface) {
                Destinations.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { onSelect(index) },
                        icon = { MonarchIcon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
        ) {
            OverviewContent(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TabletShowcase(selected: Int, onSelect: (Int) -> Unit) {
    val colors = MonarchTheme.colors
    Row(Modifier.fillMaxSize()) {
        PermanentDrawerSheet(
            modifier = Modifier.width(SidebarWidth),
            drawerContainerColor = colors.bgSurface,
            drawerContentColor = colors.textDefault,
            windowInsets = WindowInsets.safeDrawing,
        ) {
            Column(
                modifier = Modifier.padding(MonarchTheme.spacing.space3),
                verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space1),
            ) {
                MonarchText(
                    text = "Acme Studio",
                    style = MonarchTextStyle.Subheading,
                    modifier = Modifier.padding(
                        horizontal = MonarchTheme.spacing.space4,
                        vertical = MonarchTheme.spacing.space4,
                    ),
                    heading = true,
                )
                Destinations.forEachIndexed { index, destination ->
                    NavigationDrawerItem(
                        label = { Text(destination.label) },
                        selected = selected == index,
                        onClick = { onSelect(index) },
                        icon = { MonarchIcon(destination.icon, contentDescription = null) },
                        shape = MonarchTheme.shapes.md,
                    )
                }
            }
        }
        VerticalDivider(color = colors.borderSubtle)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(ContentInsets),
            contentAlignment = Alignment.TopCenter,
        ) {
            OverviewContent(Modifier.widthIn(max = ShowcaseContentMaxWidth).fillMaxWidth())
        }
    }
}

/** The Overview screen's column: title, workspace, rollout, recent activity and the main action. */
@Composable
private fun OverviewContent(modifier: Modifier = Modifier) {
    val spacing = MonarchTheme.spacing
    Column(
        modifier = modifier.padding(horizontal = spacing.space4, vertical = spacing.space5),
        verticalArrangement = Arrangement.spacedBy(spacing.space6),
    ) {
        MonarchText(text = "Overview", style = MonarchTextStyle.Display, heading = true)
        WorkspaceHeaderCard()
        Section("Workspace") { WorkspaceCard() }
        RolloutCard()
        Section("Recent activity") { RecentActivityCard() }
        MonarchButton(
            text = "Invite people",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            variant = MonarchButtonVariant.Primary,
        )
    }
}

/** A section: a heading in the muted label style above its content. */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
        MonarchText(text = title, style = MonarchTextStyle.Label, color = MonarchTheme.colors.textMuted, heading = true)
        content()
    }
}

@Composable
private fun WorkspaceHeaderCard() {
    MonarchCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
        ) {
            MonarchAvatar(
                image = null,
                name = "Acme Studio",
                shape = MonarchAvatarShape.Square,
                size = MonarchAvatarSize.Md,
                decorative = true,
            )
            Column(Modifier.weight(1f)) {
                MonarchText(text = "Acme Studio", style = MonarchTextStyle.Subheading)
                MonarchText(
                    text = "Business plan · 12 seats",
                    style = MonarchTextStyle.Caption,
                    color = MonarchTheme.colors.textMuted,
                )
            }
            MonarchBadge(text = "Active", tone = MonarchTone.Success)
        }
    }
}

@Composable
private fun WorkspaceCard() {
    var name by rememberSaveable { mutableStateOf("Acme Studio") }
    var region by rememberSaveable { mutableStateOf(Regions.first()) }
    var digest by rememberSaveable { mutableStateOf(true) }
    // The switch row ends in its own touch-target padding, so the card's bottom padding is trimmed by as much.
    MonarchCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = trimmedBottomPadding(MonarchTheme.spacing.space1),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4)) {
            MonarchTextField(
                value = name,
                onValueChange = { name = it },
                label = "Workspace name",
                modifier = Modifier.fillMaxWidth(),
            )
            MonarchSelect(
                options = Regions.map { MonarchSelectOption(it, it) },
                selected = region,
                onSelect = { region = it },
                label = "Region",
                modifier = Modifier.fillMaxWidth(),
            )
            DigestSwitchRow(checked = digest, onCheckedChange = { digest = it })
        }
    }
}

/** The card padding with a smaller [bottom], for content that ends in a touch target's padding. */
@Composable
internal fun trimmedBottomPadding(bottom: Dp): PaddingValues {
    // The card's own padding is space5 (20).
    val side = MonarchTheme.spacing.space5
    return PaddingValues(start = side, top = side, end = side, bottom = bottom)
}

/** The Weekly digest setting, shared by the screen and the toggle float. */
@Composable
internal fun DigestSwitchRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    MonarchSwitchRow(
        checked = checked,
        onCheckedChange = onCheckedChange,
        label = "Weekly digest",
        description = "Sent on Monday mornings.",
    )
}

@Composable
private fun RolloutCard() {
    // The small button's 48 dp touch target adds space under the last row; trim the card's bottom padding to match.
    MonarchCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = trimmedBottomPadding(MonarchTheme.spacing.space3),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4)) {
            RolloutSummary()
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonarchAvatarGroup(
                    items = ReleaseTeam,
                    label = "Release team",
                    maxVisible = 3,
                    size = MonarchAvatarSize.Sm,
                ) { person ->
                    MonarchAvatar(image = null, name = person, size = MonarchAvatarSize.Sm)
                }
                Spacer(Modifier.weight(1f))
                MonarchButton(
                    text = "View rollout",
                    onClick = {},
                    variant = MonarchButtonVariant.Secondary,
                    size = MonarchButtonSize.Sm,
                )
            }
        }
    }
}

/** Release 2.4 with its status and progress, shared by the screen and the progress float. */
@Composable
internal fun RolloutSummary() {
    Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MonarchText(text = "Release 2.4", style = MonarchTextStyle.Subheading, modifier = Modifier.weight(1f))
            MonarchBadge(text = "Rolling out", tone = MonarchTone.Info)
        }
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            MonarchText(
                text = "3 of 5 regions",
                style = MonarchTextStyle.Caption,
                color = MonarchTheme.colors.textMuted,
            )
            MonarchProgress(progress = RolloutProgress, label = "3 of 5 regions", modifier = Modifier.fillMaxWidth())
        }
    }
}

/** The Recent activity card: list rows flush with the card's edges. */
@Composable
internal fun RecentActivityCard(modifier: Modifier = Modifier, items: List<ActivityItem> = RecentActivity) {
    MonarchCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(MonarchTheme.spacing.space0)) {
        items.forEachIndexed { index, item ->
            MonarchListRow(
                headlineContent = { Text(item.title) },
                supportingContent = { Text(item.detail) },
                trailingContent = { MonarchBadge(text = item.status, tone = item.tone) },
                divider = index < items.lastIndex,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun ShowcasePhonePreview() {
    ShowcaseApp(dark = false)
}

@Preview(showBackground = true, widthDp = 800, heightDp = 1280)
@Composable
private fun ShowcaseTabletPreview() {
    ShowcaseApp(dark = true)
}
