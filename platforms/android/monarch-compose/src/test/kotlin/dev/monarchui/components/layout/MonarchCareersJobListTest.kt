package dev.monarchui.components.layout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.layout.careersjoblist.MonarchCareerJob
import dev.monarchui.components.layout.careersjoblist.MonarchCareersJobList
import dev.monarchui.components.layout.careersjoblist.MonarchCareersJobListLabels
import dev.monarchui.components.layout.careersjoblist.MonarchCareersJobListTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

private val Designer = MonarchCareerJob(
    id = "j1",
    title = "Product Designer",
    department = "Design",
    location = "Remote",
    employmentType = "Full time",
    experience = "3+ years",
    salary = "USD 90,000 – 120,000",
    summary = "Shape how teams plan and track their work, from research to shipped flows.",
    responsibilities = listOf("Design new flows end to end"),
    mustHaveSkills = listOf("Prototyping", "User research"),
    applyLink = "apply-designer",
)
private val Support = MonarchCareerJob(
    id = "j2",
    title = "Support Specialist",
    department = "Customer support",
    location = "Lisbon",
    employmentType = "Part time",
    mustHaveSkills = listOf("Written English"),
)

internal object MonarchCareersJobListFixtures : ComponentFixtures {
    override val id = "careers-job-list"
    override val fixtures = listOf(
        fixture("two roles", tall = true) {
            MonarchCareersJobList(listOf(Designer, Support), openJob = null, onOpenJobChange = {}, onApply = {})
        },
        fixture("one role, one column") {
            MonarchCareersJobList(listOf(Support), openJob = null, onOpenJobChange = {}, onApply = {}, columns = 1)
        },
        fixture("no roles") {
            MonarchCareersJobList(emptyList(), openJob = null, onOpenJobChange = {}, onApply = {})
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCareersJobListTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theHeadingCountsThePositions() {
        rule.setMonarchContent { MonarchCareersJobList(listOf(Designer, Support), null, {}, {}) }
        rule.onNodeWithText("Open roles").assert(isHeading())
        rule.onNodeWithText("2 positions").assertExists()
    }

    @Test
    fun oneRoleIsOnePosition() {
        rule.setMonarchContent { MonarchCareersJobList(listOf(Support), null, {}, {}) }
        rule.onNodeWithText("1 position").assertExists()
    }

    @Test
    fun withoutRolesTheEmptyMessageIsShown() {
        rule.setMonarchContent { MonarchCareersJobList(
            emptyList(),
            null,
            {},
            {},
            labels = MonarchCareersJobListLabels(title = "Internships"),
        ) }
        rule.onNodeWithText("Internships").assertExists()
        rule.onNodeWithText("No open roles right now. Check back soon.").assertExists()
    }

    @Test
    fun aCardOpensTheDetailsAndApplyCallsBack() {
        var applied = ""
        rule.setMonarchContent {
            var open by remember { mutableStateOf<String?>(null) }
            MonarchCareersJobList(
                listOf(Designer, Support),
                open,
                { open = it },
                { applied = it.id },
                organizationName = "Northwind",
            )
        }
        rule.onNodeWithText("Product Designer", substring = true).assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Applying to Northwind").performScrollTo().assertExists()
        rule.onNodeWithText("Apply now").performScrollTo().performClick()
        assertEquals("j1", applied)
    }

    @Test
    fun aRoleWithoutALinkIsAppliedToByEmail() {
        rule.setMonarchContent { MonarchCareersJobList(
            listOf(Support),
            "j2",
            {},
            {},
            contactEmail = "jobs@example.com",
        ) }
        rule.waitForIdle()
        rule.onNodeWithText("Apply by email").assertExists()
    }

    @Test
    fun theCardReadsItsDepartmentInItsOwnCase() {
        rule.setMonarchContent { MonarchCareersJobList(listOf(Designer), null, {}, {}) }
        rule.onNodeWithText("Product Designer", substring = true).assert(hasText("Design"))
    }

    @Test
    fun theCountFollowsTheHeadingInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchCareersJobList(listOf(Designer), null, {}, {}) }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Open roles").assertIsBefore(rule.onNodeWithText("1 position"), direction)
        }
    }

    @Test
    fun theTitleWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchCareersJobList(listOf(Designer), null, {}, {}) }
        rule.onNodeWithText("Product Designer", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun metadataIsMutedInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchCareersJobListTokens.muted(colors), theme.id)
        }
    }
}
