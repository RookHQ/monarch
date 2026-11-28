package com.rookhq.monarch.components.layout

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.layout.logocloud.MonarchLogoCloud
import com.rookhq.monarch.components.layout.logocloud.MonarchLogoCloudLayout
import com.rookhq.monarch.components.layout.logocloud.MonarchLogoCloudLogo
import com.rookhq.monarch.components.layout.logocloud.MonarchLogoCloudSize
import com.rookhq.monarch.components.layout.logocloud.MonarchLogoCloudTokens
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
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

private val Names = listOf("Company A", "Company B", "Company C", "Company D", "Company E", "Company F")

internal object MonarchLogoCloudFixtures : ComponentFixtures {
    override val id = "logo-cloud"
    override val fixtures = listOf(
        fixture("wordmarks in a row") {
            MonarchLogoCloud(logos = Names.map { MonarchLogoCloudLogo(it) }, title = "Placeholder organisations")
        },
        fixture("grid with links, small") {
            MonarchLogoCloud(
                logos = Names.take(4).map { MonarchLogoCloudLogo(it, link = it) },
                title = "Integrations",
                layout = MonarchLogoCloudLayout.Grid,
                size = MonarchLogoCloudSize.Sm,
                onLogoClick = {},
            )
        },
        fixture("large, no title") {
            MonarchLogoCloud(logos = Names.take(2).map { MonarchLogoCloudLogo(it) }, size = MonarchLogoCloudSize.Lg)
        },
        fixture("empty") { MonarchLogoCloud(logos = emptyList(), title = "Customers") },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchLogoCloudTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsAHeadingAndNamesTheGroup() {
        rule.setMonarchContent { MonarchLogoCloud(logos = Names.map { MonarchLogoCloudLogo(it) }, title = "Partners") }
        rule.onNodeWithText("Partners").assert(isHeading())
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Partners")).assertExists()
        rule.onNodeWithText("Company A").assertHasNoClickAction()
    }

    @Test
    fun withoutATitleTheGroupIsNamedOrganisationsOrTheOverride() {
        rule.setMonarchContent { MonarchLogoCloud(logos = Names.map { MonarchLogoCloudLogo(it) }) }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Organisations")).assertExists()
    }

    @Test
    fun theListLabelCanBeReplaced() {
        rule.setMonarchContent { MonarchLogoCloud(
            logos = Names.map { MonarchLogoCloudLogo(it) },
            listLabel = "Clientes",
        ) }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Clientes")).assertExists()
    }

    @Test
    fun aLinkedLogoIsAButtonThatCallsBack() {
        var opened = ""
        rule.setMonarchContent {
            MonarchLogoCloud(
                logos = listOf(MonarchLogoCloudLogo("Company A", link = "a"), MonarchLogoCloudLogo("Company B")),
                onLogoClick = { opened = it.link.orEmpty() },
            )
        }
        rule.onNodeWithText("Company A")
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("a", opened)
        rule.onNodeWithText("Company B").assertHasNoClickAction()
    }

    @Test
    fun logosFollowTheLayoutDirection() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchLogoCloud(logos = Names.take(2).map { MonarchLogoCloudLogo(it) }) }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Company A").assertIsBefore(rule.onNodeWithText("Company B"), direction)
        }
    }

    @Test
    fun wordmarksWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchLogoCloud(
                logos = listOf(MonarchLogoCloudLogo("Northwind Traders International")),
                title = "Customers",
            )
        }
        rule.onNodeWithText("Northwind Traders International").assertTextFullyVisible()
    }

    @Test
    fun wordmarksAreQuietInEveryTheme() {
        assertEquals(MonarchTextStyle.BodyStrong, MonarchLogoCloudTokens.wordmarkStyle(MonarchLogoCloudSize.Md))
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchLogoCloudTokens.wordmark(colors), theme.id)
            assertEquals(colors.borderDefault, MonarchLogoCloudTokens.cellBorder(colors), theme.id)
        }
    }
}
