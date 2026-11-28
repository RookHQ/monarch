package com.rookhq.monarch.components.interaction.themetoggle

import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.forms.segmentedcontrol.MonarchSegmentedControlSize
import com.rookhq.monarch.theme.MonarchScheme

/** Component tokens of MonarchThemeToggle, built from primitives and roles only. */
internal object MonarchThemeToggleTokens {
    /** The choices in the order the web and iOS show them. */
    val Choices: List<MonarchScheme> = listOf(MonarchScheme.Light, MonarchScheme.Dark, MonarchScheme.System)

    /** The Monarch segmented control size of the segments at [size]. */
    fun segmentedSize(size: MonarchThemeToggleSize): MonarchSegmentedControlSize = when (size) {
        MonarchThemeToggleSize.Sm -> MonarchSegmentedControlSize.Sm
        MonarchThemeToggleSize.Md -> MonarchSegmentedControlSize.Md
    }

    /** The Monarch button size of the menu button at [size]. */
    fun buttonSize(size: MonarchThemeToggleSize): MonarchButtonSize = when (size) {
        MonarchThemeToggleSize.Sm -> MonarchButtonSize.Sm
        MonarchThemeToggleSize.Md -> MonarchButtonSize.Md
    }
}
