package com.rookhq.monarch.foundation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

internal object MonarchIconFixtures : ComponentFixtures {
    override val id = "icon"
    override val fixtures = listOf(
        fixture("every size") {
            Row {
                MonarchIconSize.entries.forEach { size ->
                    MonarchIcon(imageVector = TestIcon, contentDescription = size.name, size = size)
                }
            }
        },
        fixture("tinted, decorative") {
            MonarchIcon(imageVector = TestIcon, contentDescription = null, tint = MonarchTheme.colors.accentStrong)
        },
        fixture("painter, mirrored in right to left") {
            MonarchIcon(painter = rememberVectorPainter(TestIcon), contentDescription = "Back", mirrorInRtl = true)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchIconTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun iconsHaveTheMonarchSizesAndAnImageRoleWhenDescribed() {
        rule.setMonarchContent {
            Column {
                MonarchIconSize.entries.forEach { size ->
                    MonarchIcon(imageVector = TestIcon, contentDescription = size.name, size = size)
                }
            }
        }
        val expected = mapOf(MonarchIconSize.Sm to 16.dp, MonarchIconSize.Md to 20.dp, MonarchIconSize.Lg to 24.dp)
        expected.forEach { (size, dp) ->
            rule.onNodeWithContentDescription(size.name)
                .assertWidthIsEqualTo(dp)
                .assertHeightIsEqualTo(dp)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        }
    }

    @Test
    fun aDecorativeIconIsHiddenFromTalkBack() {
        rule.setMonarchContent {
            MonarchIcon(imageVector = TestIcon, contentDescription = null, modifier = Modifier.testTag("icon"))
        }
        rule.onNodeWithTag("icon")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }
}
