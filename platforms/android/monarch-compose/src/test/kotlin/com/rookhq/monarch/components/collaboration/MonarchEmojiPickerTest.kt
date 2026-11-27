package com.rookhq.monarch.components.collaboration

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.emoji2.emojipicker.EmojiViewItem
import com.rookhq.monarch.components.collaboration.emojipicker.MonarchEmojiPickedListener
import com.rookhq.monarch.components.collaboration.emojipicker.MonarchEmojiPicker
import com.rookhq.monarch.components.collaboration.reactionbar.MonarchReactionBar
import com.rookhq.monarch.components.collaboration.reactionbar.MonarchReactionBarItem
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

private fun View.emojiPickers(): List<EmojiPickerView> = when (this) {
    is EmojiPickerView -> listOf(this)
    is ViewGroup -> (0 until childCount).flatMap { getChildAt(it).emojiPickers() }
    else -> emptyList()
}

private fun EmojiPickerView.pick(emoji: String) {
    (tag as MonarchEmojiPickedListener).accept(EmojiViewItem(emoji, emptyList()))
}

internal object MonarchEmojiPickerFixtures : ComponentFixtures {
    override val id = "emoji-picker"
    override val fixtures = listOf(
        fixture("default grid") { MonarchEmojiPicker(onEmojiPicked = {}) },
        fixture("six columns, three and a half rows, named") {
            MonarchEmojiPicker(onEmojiPicked = {}, columns = 6, rows = 3.5f, label = "Choose a reaction")
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchEmojiPickerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theSystemPickerIsHostedAndNamedForTalkBack() {
        lateinit var root: View
        rule.setMonarchContent {
            root = LocalView.current
            MonarchEmojiPicker(onEmojiPicked = {})
        }
        rule.waitForIdle()
        assertEquals(1, root.emojiPickers().size)
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Emoji")).assertExists()
    }

    @Test
    fun theLabelCanBeReplaced() {
        rule.setMonarchContent { MonarchEmojiPicker(onEmojiPicked = {}, label = "Choose a reaction") }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Choose a reaction")).assertExists()
    }

    @Test
    fun thePickedEmojiGoesToTheLatestCallback() {
        lateinit var root: View
        val first = mutableListOf<String>()
        val second = mutableListOf<String>()
        var useSecond by mutableStateOf(false)
        rule.setMonarchContent {
            root = LocalView.current
            MonarchEmojiPicker(onEmojiPicked = { if (useSecond) second += it else first += it })
        }
        rule.waitForIdle()
        val picker = root.emojiPickers().single()
        rule.runOnIdle { picker.pick("👍") }
        useSecond = true
        rule.waitForIdle()
        rule.runOnIdle { picker.pick("👍🏽") }
        assertEquals(listOf("👍"), first)
        assertEquals(listOf("👍🏽"), second)
    }

    @Test
    fun theGridSizeIsPassedToTheView() {
        lateinit var root: View
        rule.setMonarchContent {
            root = LocalView.current
            MonarchEmojiPicker(onEmojiPicked = {}, columns = 6, rows = 3.5f)
        }
        rule.waitForIdle()
        val picker = root.emojiPickers().single()
        assertEquals(6, picker.emojiGridColumns)
        assertEquals(3.5f, picker.emojiGridRows)
    }

    @Test
    fun theReactionBarAddsTheEmojiPickedInThePicker() {
        lateinit var root: View
        val toggled = mutableListOf<Pair<String, Boolean>>()
        rule.setMonarchContent {
            root = LocalView.current
            Column {
                MonarchReactionBar(
                    reactions = listOf(MonarchReactionBarItem("👍", count = 1)),
                    onToggle = { emoji, on -> toggled += emoji to on },
                    picker = { onPick, _ -> MonarchEmojiPicker(onEmojiPicked = onPick) },
                )
            }
        }
        rule.onNodeWithContentDescription("Add reaction").performClick()
        rule.waitForIdle()
        val picker = root.emojiPickers().single()
        rule.runOnIdle { picker.pick("🎉") }
        rule.waitForIdle()
        assertEquals(listOf("🎉" to true), toggled)
        // Picking closes the picker.
        assertEquals(0, root.emojiPickers().size)
    }
}
