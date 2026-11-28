package com.rookhq.monarch.components.technical

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.rookhq.monarch.components.technical.dnsrecordtable.MonarchDnsRecord
import com.rookhq.monarch.components.technical.dnsrecordtable.MonarchDnsRecordPurpose
import com.rookhq.monarch.components.technical.dnsrecordtable.MonarchDnsRecordTable
import com.rookhq.monarch.components.technical.dnsrecordtable.MonarchDnsRecordTableLabels
import com.rookhq.monarch.components.technical.dnsrecordtable.MonarchDnsRecordTableLayout
import com.rookhq.monarch.components.technical.dnsrecordtable.MonarchDnsRecordTableTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val cname = MonarchDnsRecord(
    type = "CNAME",
    name = "book.example.com",
    value = "domains.example.net",
    purpose = MonarchDnsRecordPurpose.Routing,
    found = true,
)
private val txt = MonarchDnsRecord(
    type = "TXT",
    name = "_verify.book.example.com",
    value = "verify=4f7k2m9q8d3r6t1w",
    purpose = MonarchDnsRecordPurpose.Verification,
    ttl = "3600",
    found = false,
)
private val mx = MonarchDnsRecord(type = "MX", name = "example.com", value = "mx.example.net", priority = 10)

internal object MonarchDnsRecordTableFixtures : ComponentFixtures {
    override val id = "dns-record-table"
    override val fixtures = listOf(
        fixture("card, found, zone") { MonarchDnsRecordTable(records = listOf(cname), zone = "example.com") },
        fixture("card, waiting, TTL, icon copy") {
            MonarchDnsRecordTable(
                records = listOf(MonarchDnsRecord("TXT", "_verify", "v=1", ttl = "3600", found = false)),
                copyIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
        fixture("card, priority, custom purpose") {
            MonarchDnsRecordTable(
                records = listOf(
                    MonarchDnsRecord("MX", "example.com", "mx1", MonarchDnsRecordPurpose.Custom("Mail"), priority = 10),
                ),
                zone = "example.com",
            )
        },
        fixture("table") {
            MonarchDnsRecordTable(
                records = listOf(cname, mx),
                zone = "example.com",
                layout = MonarchDnsRecordTableLayout.Table,
                copyIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
        fixture("table, text buttons") {
            MonarchDnsRecordTable(records = listOf(txt), layout = MonarchDnsRecordTableLayout.Table)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchDnsRecordTableTest {
    @get:Rule
    val rule = createComposeRule()

    private fun clipboardText(): String? {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return context.getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.text?.toString()
    }

    private fun descriptionIs(text: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(text))

    @Test
    fun namesAreShownRelativeToTheZone() {
        val t = MonarchDnsRecordTableTokens
        assertEquals("book", t.relativeName("book.example.com", "example.com"))
        assertEquals("@", t.relativeName("Example.com.", "example.COM"))
        assertEquals("_verify.book", t.relativeName("_verify.book.example.com.", "example.com."))
        assertEquals("book.other.com", t.relativeName("book.other.com", "example.com"))
        assertEquals("book.notexample.com", t.relativeName("book.notexample.com", "example.com"))
        assertEquals("book.example.com", t.relativeName("book.example.com", null))
        assertEquals("book.example.com", t.relativeName("book.example.com", ""))
    }

    @Test
    fun theAutomaticLayoutIsATableOnlyWhenWideAndAtSmallFontScales() {
        val t = MonarchDnsRecordTableTokens
        assertTrue(t.showsTable(MonarchDnsRecordTableLayout.Automatic, 640.dp, 1f))
        assertFalse(t.showsTable(MonarchDnsRecordTableLayout.Automatic, 639.dp, 1f))
        assertFalse(t.showsTable(MonarchDnsRecordTableLayout.Automatic, 900.dp, LargestFontScale))
        assertTrue(t.showsTable(MonarchDnsRecordTableLayout.Table, 300.dp, LargestFontScale))
        assertFalse(t.showsTable(MonarchDnsRecordTableLayout.Cards, 900.dp, 1f))
    }

    @Test
    fun countsAndStatusesFollowTheFoundState() {
        val t = MonarchDnsRecordTableTokens
        val counts = t.counts(listOf(cname, txt, mx))
        assertEquals(1, counts.found)
        assertEquals(2, counts.checked)
        assertEquals(MonarchTone.Success, t.status(cname, "Found", "Waiting")?.tone)
        assertEquals("Waiting", t.status(txt, "Found", "Waiting")?.label)
        assertEquals(MonarchTone.Neutral, t.status(txt, "Found", "Waiting")?.tone)
        assertNull(t.status(mx, "Found", "Waiting"))
        val columns = t.columns(listOf(cname, txt))
        assertTrue(columns.ttl)
        assertTrue(columns.status)
        assertFalse(columns.priority)
        assertFalse(t.columns(listOf(mx)).status)
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            assertEquals(colors.bgSurface, MonarchDnsRecordTableTokens.surface(colors))
            assertEquals(colors.borderDefault, MonarchDnsRecordTableTokens.border(colors))
            assertEquals(colors.textMuted, MonarchDnsRecordTableTokens.muted(colors))
            assertEquals(colors.textDefault, MonarchDnsRecordTableTokens.text(colors))
        }
    }

    @Test
    fun aCardHasAHeadingAStatusWordAndSpokenCopyFields() {
        rule.setMonarchContent { MonarchDnsRecordTable(records = listOf(cname), zone = "example.com") }
        rule.onNodeWithContentDescription("DNS records for example.com").assertExists()
        rule.onNodeWithText("Points your address here").assert(isHeading())
        rule.onNodeWithText("Found").assertExists()
        rule.onNode(descriptionIs("name of the CNAME record, book")).assertExists()
        rule.onNode(descriptionIs("value of the CNAME record book, domains.example.net")).assertExists()
        rule.onNodeWithText("Full name: book.example.com").assertExists()
        // The terms of copy fields are not read again; the type is read with its term.
        rule.onAllNodesWithText("Name").assertCountEquals(0)
        rule.onNodeWithText("Type", substring = false, useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Copy name of the CNAME record").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun copyingANameCopiesTheShownNameAndCopyingAValueCopiesItInFull() {
        val copied = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchDnsRecordTable(records = listOf(cname), zone = "example.com", onCopy = { copied += it })
        }
        rule.onNodeWithContentDescription("Copy name of the CNAME record").performClick()
        rule.waitForIdle()
        assertEquals("book", clipboardText())
        rule.onNodeWithContentDescription("Copy value of the CNAME record book").performClick()
        rule.waitForIdle()
        assertEquals("domains.example.net", clipboardText())
        assertEquals(listOf("book", "domains.example.net"), copied)
    }

    @Test
    fun theTableHasHeadingsAndOnlyTheColumnsInUse() {
        rule.setMonarchContent {
            MonarchDnsRecordTable(
                records = listOf(cname, mx),
                zone = "example.com",
                layout = MonarchDnsRecordTableLayout.Table,
            )
        }
        rule.onNodeWithText("Type").assert(isHeading())
        rule.onNodeWithText("Value").assert(isHeading())
        rule.onNodeWithText("Priority").assert(isHeading())
        rule.onNodeWithText("Status").assert(isHeading())
        rule.onAllNodesWithText("TTL").assertCountEquals(0)
        rule.onNode(descriptionIs("name of the MX record, @")).assertExists()
        rule.onAllNodesWithText("Full name: book.example.com").assertCountEquals(0)
    }

    @Test
    fun theAutomaticLayoutDrawsATableWhenWide() {
        rule.setMonarchContent {
            MonarchDnsRecordTable(
                records = listOf(cname),
                modifier = Modifier.requiredWidth(700.dp),
                zone = "example.com",
            )
        }
        rule.onNodeWithText("Status").assert(isHeading())
        rule.onAllNodesWithText("Points your address here").assertCountEquals(0)
    }

    @Test
    fun aChangeInFoundRecordsIsAnnouncedPolitely() {
        var records by mutableStateOf(listOf(cname, txt))
        rule.setMonarchContent { MonarchDnsRecordTable(records = records) }
        rule.onAllNodesWithText("1 of 2 records found.").assertCountEquals(0)
        records = listOf(cname, MonarchDnsRecord(txt.type, txt.name, txt.value, found = true))
        rule.waitForIdle()
        rule.onNodeWithContentDescription("2 of 2 records found.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchDnsRecordTable(
                records = listOf(cname),
                labels = MonarchDnsRecordTableLabels(
                    found = "Gefunden",
                    purpose = { "Weiterleitung" },
                    copyName = { record, shown -> "Name ${record.type} $shown" },
                    label = { "DNS-Einträge" },
                ),
            )
        }
        rule.onNodeWithText("Gefunden").assertExists()
        rule.onNodeWithText("Weiterleitung").assertExists()
        rule.onNodeWithContentDescription("DNS-Einträge").assertExists()
        rule.onNodeWithContentDescription("Copy Name CNAME book.example.com").assertExists()
    }

    @Test
    fun theCopyButtonFollowsItsTextInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchDnsRecordTable(records = listOf(cname), zone = "example.com") }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNode(descriptionIs("name of the CNAME record, book"))
                .assertIsBefore(rule.onNodeWithContentDescription("Copy name of the CNAME record"), direction)
        }
    }

    @Test
    fun textIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchDnsRecordTable(records = listOf(cname), zone = "example.com")
        }
        rule.onNodeWithText("Points your address here").assertTextFullyVisible()
        rule.onNodeWithText("Full name: book.example.com").assertTextFullyVisible()
    }
}
