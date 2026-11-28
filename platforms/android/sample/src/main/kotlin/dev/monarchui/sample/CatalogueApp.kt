package dev.monarchui.sample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.datadisplay.listrow.MonarchListRow
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.fonts.poppins.monarchPoppinsTypography
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTypography
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.generated.CatalogueFamilies
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

private const val NONE = -1

/** The widest the lists get on a tablet or a desktop window, so lines stay easy to read. */
private val ContentMaxWidth = 640.dp

/**
 * The catalogue: a list of families, the pages of a family, and one page per
 * component, in the theme, accent and typeface chosen on the first screen.
 *
 * The families and pages come from the generated `CatalogueFamilies`; nothing
 * is listed here. The shell itself is built from Monarch: list rows in cards,
 * radio rows, a select, an icon button and a menu.
 *
 * @param families the families to show; the generated list by default.
 * @param onDarkChanged called with whether the chosen theme is dark, so the host can style the system bars.
 * @param initialTheme the theme the catalogue opens in; the docs screenshots open it in dark as well.
 */
@Composable
fun CatalogueApp(
    families: List<CatalogueFamily> = CatalogueFamilies,
    onDarkChanged: (Boolean) -> Unit = {},
    initialTheme: CatalogueTheme = CatalogueTheme.Light,
) {
    var theme by rememberSaveable { mutableStateOf(initialTheme) }
    var accentId by rememberSaveable { mutableStateOf(MonarchTokens.Accents.rook.id) }
    var typeface by rememberSaveable { mutableStateOf(CatalogueTypeface.System) }
    var familyIndex by rememberSaveable { mutableIntStateOf(NONE) }
    var pageIndex by rememberSaveable { mutableIntStateOf(NONE) }
    val accent = MonarchTokens.Accents.all.first { it.id == accentId }
    val tokens = theme.tokens.withAccent(accent)
    val family = families.getOrNull(familyIndex)
    val page = family?.pages?.getOrNull(pageIndex)
    SideEffect { onDarkChanged(tokens.isDark) }

    // The system back gesture, with its predictive animation, goes up one level.
    BackHandler(enabled = family != null) {
        if (page != null) pageIndex = NONE else familyIndex = NONE
    }

    MonarchTheme(tokens = tokens, typography = typeface.typography()) {
        Scaffold(
            containerColor = MonarchTheme.colors.bgCanvas,
            contentColor = MonarchTheme.colors.textDefault,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                if (family != null) {
                    CatalogueTopBar(
                        title = page?.title ?: family.title,
                        onBack = { if (page != null) pageIndex = NONE else familyIndex = NONE },
                        theme = theme,
                        onThemeChange = { theme = it },
                    )
                }
            },
        ) { innerPadding ->
            when {
                page != null -> PageScreen(page, innerPadding)
                family != null -> FamilyScreen(family, innerPadding) { pageIndex = it }
                else -> HomeScreen(
                    families = families,
                    innerPadding = innerPadding,
                    appearance = {
                        AppearancePicker(
                            theme = theme,
                            onThemeChange = { theme = it },
                            accent = accent,
                            onAccentChange = { accentId = it.id },
                            typeface = typeface,
                            onTypefaceChange = { typeface = it },
                        )
                    },
                    onOpen = { familyIndex = it },
                )
            }
        }
    }
}

/** The typefaces the catalogue can show: the system font (Monarch's default) or Poppins. */
enum class CatalogueTypeface(val label: String) {
    System("System"),
    Poppins("Poppins"),
    ;

    /** The type roles of this typeface. */
    fun typography(): MonarchTypography = when (this) {
        System -> MonarchTypography.Default
        Poppins -> monarchPoppinsTypography()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogueTopBar(
    title: String,
    onBack: () -> Unit,
    theme: CatalogueTheme,
    onThemeChange: (CatalogueTheme) -> Unit,
) {
    val colors = MonarchTheme.colors
    TopAppBar(
        title = {
            MonarchText(text = title, style = MonarchTextStyle.Heading, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        navigationIcon = {
            MonarchIconButton(label = "Back", onClick = onBack) {
                MonarchIcon(SampleIcons.ArrowBack, contentDescription = null)
            }
        },
        actions = { ThemeMenu(theme = theme, onThemeChange = onThemeChange) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.bgCanvas,
            scrolledContainerColor = colors.bgCanvas,
            titleContentColor = colors.textDefault,
            navigationIconContentColor = colors.textDefault,
            actionIconContentColor = colors.textMuted,
        ),
    )
}

@Composable
private fun HomeScreen(
    families: List<CatalogueFamily>,
    innerPadding: PaddingValues,
    appearance: @Composable () -> Unit,
    onOpen: (Int) -> Unit,
) {
    val pages = families.sumOf { it.pages.size }
    CatalogueColumn(innerPadding) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space1)) {
            MonarchText(text = "Monarch catalogue", style = MonarchTextStyle.Display, heading = true)
            MonarchText(
                text = "$pages components in ${families.size} families, in Jetpack Compose.",
                color = MonarchTheme.colors.textMuted,
            )
        }
        appearance()
        CatalogueGroup("Families") {
            MonarchCard(contentPadding = PaddingValues(MonarchTheme.spacing.space0)) {
                families.forEachIndexed { index, family ->
                    MonarchListRow(
                        headlineContent = { MonarchText(family.title) },
                        supportingContent = {
                            MonarchText(if (family.pages.size == 1) "1 page" else "${family.pages.size} pages")
                        },
                        trailingContent = { Chevron() },
                        onClick = { onOpen(index) },
                        divider = index < families.lastIndex,
                    )
                }
            }
        }
    }
}

@Composable
private fun FamilyScreen(family: CatalogueFamily, innerPadding: PaddingValues, onOpen: (Int) -> Unit) {
    CatalogueColumn(innerPadding) {
        MonarchCard(contentPadding = PaddingValues(MonarchTheme.spacing.space0)) {
            family.pages.forEachIndexed { index, page ->
                MonarchListRow(
                    headlineContent = { MonarchText(page.title) },
                    trailingContent = { Chevron() },
                    onClick = { onOpen(index) },
                    divider = index < family.pages.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun PageScreen(page: CataloguePage, innerPadding: PaddingValues) {
    CatalogueColumn(innerPadding, gap = MonarchTheme.spacing.space3) { page.Content() }
}

/** A scrolling column with the page margins, centred and at most [ContentMaxWidth] wide. */
@Composable
private fun CatalogueColumn(
    innerPadding: PaddingValues,
    gap: Dp = MonarchTheme.spacing.space6,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(innerPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = MonarchTheme.spacing.space4, vertical = MonarchTheme.spacing.space5),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            content()
        }
    }
}

/** A group of the home screen: a heading in the muted label style above its card. */
@Composable
internal fun CatalogueGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
        MonarchText(
            text = title,
            style = MonarchTextStyle.Label,
            color = MonarchTheme.colors.textMuted,
            heading = true,
        )
        content()
    }
}

@Composable
private fun Chevron() {
    MonarchIcon(
        SampleIcons.ChevronRight,
        contentDescription = null,
        size = MonarchIconSize.Sm,
        tint = MonarchTheme.colors.textSubtle,
    )
}

@Composable
private fun ThemeMenu(theme: CatalogueTheme, onThemeChange: (CatalogueTheme) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        MonarchIconButton(label = "Theme", onClick = { expanded = true }) {
            MonarchIcon(SampleIcons.Palette, contentDescription = null)
        }
        MonarchMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CatalogueTheme.entries.forEach { option ->
                MonarchMenuItem(
                    text = option.label,
                    selected = option == theme,
                    onClick = {
                        onThemeChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CatalogueAppPreview() {
    CatalogueApp()
}
