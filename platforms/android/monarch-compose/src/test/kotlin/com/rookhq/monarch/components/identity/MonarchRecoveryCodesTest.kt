package com.rookhq.monarch.components.identity

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.VerbatimTtsAnnotation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import com.rookhq.monarch.components.identity.recoverycodes.MonarchRecoveryCode
import com.rookhq.monarch.components.identity.recoverycodes.MonarchRecoveryCodes
import com.rookhq.monarch.components.identity.recoverycodes.MonarchRecoveryCodesLabels
import com.rookhq.monarch.components.identity.recoverycodes.MonarchRecoveryCodesTokens
import com.rookhq.monarch.components.identity.recoverycodes.writeRecoveryCodes
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

private val Codes = listOf(
    MonarchRecoveryCode("4f7k-2m9q"),
    MonarchRecoveryCode("8d3r-6t1w", used = true),
    MonarchRecoveryCode("7n4h-3j8k"),
    MonarchRecoveryCode("2p6s-5l0z"),
)

internal object MonarchRecoveryCodesFixtures : ComponentFixtures {
    override val id = "recovery-codes"
    override val fixtures = listOf(
        fixture("codes with one used") { MonarchRecoveryCodes(codes = Codes.take(2)) },
        fixture("hidden with toggle") { MonarchRecoveryCodes(codes = Codes.take(2), initiallyRevealed = false) },
        fixture("all used") { MonarchRecoveryCodes(codes = listOf(MonarchRecoveryCode("4f7k-2m9q", used = true))) },
        fixture("regenerate") { MonarchRecoveryCodes(codes = Codes.take(1), onRegenerate = {}) },
        fixture("acknowledgement") {
            MonarchRecoveryCodes(
                codes = Codes.take(1),
                initiallyRevealed = false,
                showRevealToggle = false,
                onAcknowledge = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchRecoveryCodesTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun codesAreSpelledAndUsedCodesSayUsed() {
        rule.setMonarchContent { MonarchRecoveryCodes(codes = Codes) }
        val code = rule.onNodeWithText("4f7k-2m9q").fetchSemanticsNode()
        val text = code.config[SemanticsProperties.Text].single()
        val verbatim = text.getTtsAnnotations(0, text.length).map { it.item }
        assertEquals(listOf<Any>(VerbatimTtsAnnotation("4f7k-2m9q")), verbatim)
        assertEquals("Used", rule.onNodeWithText("8d3r-6t1w").stateDescription())
        rule.onNodeWithText("3 of 4 codes left").assertExists()
        rule.onNodeWithContentDescription("Recovery codes").assertExists()
    }

    @Test
    fun saveOpensTheShareSheetWithTheUnusedCodes() {
        rule.setMonarchContent { MonarchRecoveryCodes(codes = Codes, heading = "Rook account") }
        rule.onNodeWithText("Save or share").assertTouchTargetAtLeast48dp(rule.density).performClick()
        val chooser = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals("Rook account\n\n4f7k-2m9q\n7n4h-3j8k\n2p6s-5l0z", send.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun onSaveReplacesTheShareSheet() {
        var saved: String? = null
        rule.setMonarchContent { MonarchRecoveryCodes(codes = Codes.take(1), onSave = { saved = it }) }
        rule.onNodeWithText("Save or share").performClick()
        assertEquals("4f7k-2m9q", saved)
    }

    @Test
    fun hiddenCodesShowAHintUntilRevealed() {
        rule.setMonarchContent { MonarchRecoveryCodes(codes = Codes, initiallyRevealed = false) }
        rule.onAllNodesWithText("4f7k-2m9q").assertCountEquals(0)
        rule.onNodeWithText("Show codes").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("4f7k-2m9q").assertExists()
        rule.onNodeWithText("Hide codes").assertExists()
    }

    @Test
    fun copyAndSaveAreDisabledWhenEveryCodeIsUsed() {
        rule.setMonarchContent { MonarchRecoveryCodes(codes = listOf(MonarchRecoveryCode("a", used = true))) }
        rule.onNodeWithContentDescription("Copy codes").assertIsNotEnabled()
        rule.onNodeWithText("Save or share").assertIsNotEnabled()
    }

    @Test
    fun regeneratingAsksFirstThenAnnouncesTheNewCodes() {
        var codes by mutableStateOf(Codes.take(1))
        rule.setMonarchContent {
            MonarchRecoveryCodes(codes = codes, onRegenerate = { codes = listOf(MonarchRecoveryCode("9a1e-4u7y")) })
        }
        rule.onNodeWithText("Generate new codes").performClick()
        rule.onNode(isDialog()).assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("4f7k-2m9q").assertExists()
        rule.onNodeWithText("Generate new codes").performClick()
        rule.onAllNodesWithText("Generate new codes")[1].performClick()
        rule.waitForIdle()
        rule.onNodeWithText("9a1e-4u7y").assertExists()
        rule.onNodeWithContentDescription("New recovery codes generated. The old ones no longer work.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun continueNeedsTheAcknowledgement() {
        var acknowledged = 0
        rule.setMonarchContent { MonarchRecoveryCodes(codes = Codes.take(1), onAcknowledge = { acknowledged++ }) }
        rule.onNodeWithText("Continue").performClick()
        assertEquals(0, acknowledged)
        rule.onNodeWithText("Confirm that you have saved the codes.").assertExists()
        rule.onNodeWithContentDescription("Confirm that you have saved the codes.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("I have saved my recovery codes").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Continue").performClick()
        assertEquals(1, acknowledged)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchRecoveryCodes(
                codes = Codes,
                labels = MonarchRecoveryCodesLabels(save = "Guardar", remaining = { left, total -> "$left/$total" }),
            )
        }
        rule.onNodeWithText("Guardar").assertExists()
        rule.onNodeWithText("3/4").assertExists()
    }

    @Test
    fun theFirstCodeIsAtTheStartRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { MonarchRecoveryCodes(codes = Codes.take(2)) }
        rule.onNodeWithText("4f7k-2m9q").assertIsBefore(rule.onNodeWithText("8d3r-6t1w"), LayoutDirection.Rtl)
    }

    @Test
    fun codesAreNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchRecoveryCodes(codes = Codes.take(2)) }
        rule.onNodeWithText("4f7k-2m9q").assertTextFullyVisible()
    }

    @Test
    fun theLogicOfTextColumnsAndToggle() {
        val t = MonarchRecoveryCodesTokens
        assertEquals("4f7k-2m9q\n7n4h-3j8k\n2p6s-5l0z", t.text(Codes, heading = null))
        assertEquals(2, t.columns(288.dp, 1f))
        assertEquals(1, t.columns(288.dp, LargestFontScale))
        assertEquals(1, t.columns(10.dp, 1f))
        assertTrue(t.showsRevealToggle(null, initiallyRevealed = false))
        assertFalse(t.showsRevealToggle(null, initiallyRevealed = true))
        assertFalse(t.anyUnused(listOf(MonarchRecoveryCode("a", used = true))))
    }

    @Test
    fun theColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val panel = MonarchRecoveryCodesTokens.panelColors(c)
            assertEquals(c.bgSubtle, panel.container, theme.id)
            assertEquals(c.borderSubtle, panel.border, theme.id)
            assertEquals(c.textSubtle, MonarchRecoveryCodesTokens.codeColor(used = true, c), theme.id)
            assertEquals(c.textDefault, MonarchRecoveryCodesTokens.codeColor(used = false, c), theme.id)
        }
    }

    @Test
    fun downloadSavesTheCodesToTheFileChosenInTheSystemPicker() {
        val uri = Uri.parse("content://com.rookhq.test/recovery-codes.txt")
        val written = ByteArrayOutputStream()
        val context = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(context.contentResolver).registerOutputStream(uri, written)
        val picker = FakeFilePicker(uri)
        rule.setMonarchContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides picker) {
                MonarchRecoveryCodes(codes = Codes, heading = "Rook, ana@example.com")
            }
        }
        rule.onNodeWithText("Download").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals("recovery-codes.txt", picker.suggestedName)
        assertEquals("text/plain", picker.mimeType)
        val expected = MonarchRecoveryCodesTokens.text(Codes, "Rook, ana@example.com")
        assertEquals(expected, written.toString(Charsets.UTF_8.name()))
        rule.onNodeWithContentDescription("Recovery codes saved to a file.").assertExists()
    }

    @Test
    fun aClosedPickerDoesNothingAndAFailedWriteIsShownAndAnnounced() {
        val closed = FakeFilePicker(null)
        rule.setMonarchContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides closed) {
                MonarchRecoveryCodes(codes = Codes, fileName = "codes.txt")
            }
        }
        rule.onNodeWithText("Download").performClick()
        rule.waitForIdle()
        assertEquals("codes.txt", closed.suggestedName)
        rule.onNodeWithContentDescription("Recovery codes saved to a file.").assertDoesNotExist()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val error = runBlocking {
            writeRecoveryCodes(context, Uri.parse("content://com.rookhq.test/missing"), "x")
        }
        assertTrue(error != null)
    }
}

/** The system file picker, answered with [uri] (`null`: closed), keeping what it was asked for. */
private class FakeFilePicker(private val uri: Uri?) : ActivityResultRegistryOwner {
    var suggestedName: String? = null
    var mimeType: String? = null

    override val activityResultRegistry: ActivityResultRegistry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            suggestedName = input as String
            mimeType = contract.createIntent(ApplicationProvider.getApplicationContext(), input).type
            dispatchResult(requestCode, uri)
        }
    }
}
