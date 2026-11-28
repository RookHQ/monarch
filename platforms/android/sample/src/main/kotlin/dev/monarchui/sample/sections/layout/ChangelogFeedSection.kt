package dev.monarchui.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.navigation.link.MonarchLink
import dev.monarchui.components.navigation.link.MonarchLinkVariant
import dev.monarchui.components.typography.prosestyle.MonarchProse
import dev.monarchui.components.typography.prosestyle.MonarchProseStyle
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

private val FeedHeight = 520.dp

private class SampleRelease(
    val id: String,
    val date: Long,
    val version: String,
    val title: String,
    val tags: List<Pair<String, MonarchTone>>,
    val body: String,
    val href: String?,
)

private val Releases = listOf(
    SampleRelease(
        id = "2-4-0",
        date = 1_793_750_400_000L,
        version = "2.4.0",
        title = "Invoices show the tax number",
        tags = listOf("New" to MonarchTone.Info),
        body = "Every invoice now prints your tax number under the company name.",
        href = "https://example.com/changelog/2-4-0",
    ),
    SampleRelease(
        id = "2-3-1",
        date = 1_792_540_800_000L,
        version = "2.3.1",
        title = "Faster search",
        tags = listOf("Improved" to MonarchTone.Success, "Fix" to MonarchTone.Neutral),
        body = "Search results appear twice as fast on large workspaces, and accented names match.",
        href = null,
    ),
)

/** The catalogue page of ChangelogFeed: a What's new list of MonarchBadge, MonarchProse and MonarchLink. */
object ChangelogFeedSection : CataloguePage {
    override val title = "ChangelogFeed"

    @Composable
    override fun Content() {
        val locale = LocalConfiguration.current.locales[0]
        // Release days are dates, so they are written in UTC: the same day in every time zone.
        val dates = remember(locale) {
            DateFormat.getDateInstance(DateFormat.LONG, locale).apply { timeZone = TimeZone.getTimeZone("UTC") }
        }
        val uriHandler = LocalUriHandler.current
        CatalogueCaption("What's new, newest first")
        LazyColumn(
            modifier = Modifier.height(FeedHeight),
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space6),
        ) {
            items(Releases, key = { it.id }) { release ->
                Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                    MonarchText(
                        text = "${dates.format(Date(release.date))} · ${release.version}",
                        style = MonarchTextStyle.Caption,
                        color = MonarchTheme.colors.textMuted,
                    )
                    MonarchText(text = release.title, style = MonarchTextStyle.Heading)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                        release.tags.forEach { (label, tone) -> MonarchBadge(text = label, tone = tone) }
                    }
                    MonarchProse(style = MonarchProseStyle(limitLineLength = false)) { paragraph(release.body) }
                    release.href?.let { href ->
                        MonarchLink(
                            text = "Read more",
                            onClick = { uriHandler.openUri(href) },
                            variant = MonarchLinkVariant.Standalone,
                            external = true,
                        )
                    }
                }
            }
        }
    }
}
