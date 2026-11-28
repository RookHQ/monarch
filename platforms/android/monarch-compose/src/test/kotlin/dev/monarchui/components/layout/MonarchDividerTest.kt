package dev.monarchui.components.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerOrientation
import dev.monarchui.components.layout.divider.MonarchDividerTokens
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal object MonarchDividerFixtures : ComponentFixtures {
    override val id = "separator"
    override val fixtures = listOf(
        fixture("horizontal") {
            Column {
                MonarchText(text = "Above")
                MonarchDivider()
                MonarchText(text = "Below")
            }
        },
        fixture("vertical") {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
            ) {
                MonarchText(text = "Start")
                MonarchDivider(orientation = MonarchDividerOrientation.Vertical)
                MonarchText(text = "End")
            }
        },
        fixture("subtle") {
            Column {
                MonarchText(text = "Row one")
                MonarchDivider(variant = MonarchDividerVariant.Subtle)
                MonarchText(text = "Row two")
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchDividerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun dividersAreOneDpThick() {
        rule.setMonarchContent {
            Column {
                MonarchDivider(modifier = Modifier.testTag("horizontal"))
                Row(Modifier.height(40.dp)) {
                    MonarchDivider(
                        modifier = Modifier.testTag("vertical"),
                        orientation = MonarchDividerOrientation.Vertical,
                    )
                }
            }
        }
        rule.onNodeWithTag("horizontal").assertHeightIsEqualTo(1.dp)
        rule.onNodeWithTag("vertical").assertWidthIsEqualTo(1.dp).assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun theVariantPicksTheDefaultOrTheSubtleBorder() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val default = MonarchDividerTokens.color(MonarchDividerVariant.Default, colors)
            val subtle = MonarchDividerTokens.color(MonarchDividerVariant.Subtle, colors)
            assertEquals(colors.borderDefault, default, theme.id)
            assertEquals(colors.borderSubtle, subtle, theme.id)
        }
    }
}
