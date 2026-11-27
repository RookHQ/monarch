package com.rookhq.monarch.sampleicons

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SampleIconsTest {
    private val all = mapOf(
        "Check" to SampleIcons.Check, "Dash" to SampleIcons.Dash, "ChevronDown" to SampleIcons.ChevronDown,
        "ChevronRight" to SampleIcons.ChevronRight, "Close" to SampleIcons.Close, "Info" to SampleIcons.Info,
        "Warning" to SampleIcons.Warning, "Search" to SampleIcons.Search, "Plus" to SampleIcons.Plus,
        "Bot" to SampleIcons.Bot, "Board" to SampleIcons.Board, "Placeholder" to SampleIcons.Placeholder,
        "Users" to SampleIcons.Users, "Trash" to SampleIcons.Trash, "Settings" to SampleIcons.Settings,
    )

    @Test
    fun everyIconIsA24DpVectorWithPaths() {
        for ((name, icon) in all) {
            assertEquals(24.dp, icon.defaultWidth, name)
            assertEquals(24.dp, icon.defaultHeight, name)
            assertTrue(icon.root.size > 0, "$name has no paths")
        }
    }

    @Test
    fun theLegacyDrawableMapCoversTheTwentyMostUsedNames() {
        val top = listOf(
            "ic_media_previous", "ic_media_next", "ic_menu_close_clear_cancel", "ic_menu_view", "ic_menu_search",
            "ic_media_play", "ic_menu_more", "ic_menu_gallery", "arrow_down_float", "ic_dialog_info",
            "ic_btn_speak_now", "ic_popup_reminder", "ic_menu_agenda", "ic_menu_revert", "ic_menu_edit",
            "ic_menu_share", "ic_lock_silent_mode", "ic_menu_sort_by_size", "ic_menu_add", "star_big_on",
        )
        top.forEach { assertTrue(it in SampleIcons.ForLegacyDrawable, it) }
    }

    @Test
    fun theAiGlyphIsNotTheStar() {
        assertNotEquals(SampleIcons.Placeholder.name, SampleIcons.Bot.name)
    }
}
