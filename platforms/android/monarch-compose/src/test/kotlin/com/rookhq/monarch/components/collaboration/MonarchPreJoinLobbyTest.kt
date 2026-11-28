@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.collaboration

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.collaboration.callcontrols.MonarchCallControl
import com.rookhq.monarch.components.collaboration.prejoinlobby.MonarchPreJoinLobby
import com.rookhq.monarch.components.collaboration.prejoinlobby.MonarchPreJoinLobbyLabels
import com.rookhq.monarch.components.collaboration.prejoinlobby.MonarchPreJoinLobbyParticipant
import com.rookhq.monarch.components.collaboration.prejoinlobby.MonarchPreJoinLobbyTokens
import com.rookhq.monarch.components.typography.text.MonarchText
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
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

private val lobbyGlyph: @Composable (MonarchCallControl, Boolean) -> Unit = { _, _ ->
    Icon(TestIcon, contentDescription = null)
}

private val lobbyPeople = listOf("Ana Ruiz", "Ben Okafor", "Chloé Martin", "Dev Patel", "Eli Cohen", "Fay Wong", "Gus")
    .map { MonarchPreJoinLobbyParticipant(it) }

internal object MonarchPreJoinLobbyFixtures : ComponentFixtures {
    override val id = "pre-join-lobby"
    override val fixtures = listOf(
        fixture("microphone and camera off, cancel") {
            MonarchPreJoinLobby(
                micOn = false,
                onMicOnChange = {},
                onJoin = { _, _ -> },
                icon = lobbyGlyph,
                onCameraOnChange = {},
                onCancel = {},
            )
        },
        fixture("camera on with the app's preview") {
            MonarchPreJoinLobby(
                micOn = true,
                onMicOnChange = {},
                onJoin = { _, _ -> },
                icon = lobbyGlyph,
                cameraOn = true,
                onCameraOnChange = {},
                audioLevel = 0.6f,
                showTitle = false,
            ) { MonarchText("Camera") }
        },
        fixture("camera starting, people in the call") {
            MonarchPreJoinLobby(
                micOn = true,
                onMicOnChange = {},
                onJoin = { _, _ -> },
                icon = lobbyGlyph,
                cameraOn = true,
                onCameraOnChange = {},
                participants = lobbyPeople,
                showTitle = false,
            )
        },
        fixture("error, joining") {
            MonarchPreJoinLobby(
                micOn = true,
                onMicOnChange = {},
                onJoin = { _, _ -> },
                icon = lobbyGlyph,
                joining = true,
                showTitle = false,
                error = "Camera access is blocked. Allow it in Settings.",
                onCancel = {},
            )
        },
        fixture("audio only, custom join label") {
            MonarchPreJoinLobby(
                micOn = true,
                onMicOnChange = {},
                onJoin = { _, _ -> },
                icon = lobbyGlyph,
                joinLabel = "Start the weekly planning call",
                devices = { MonarchText("Devices") },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchPreJoinLobbyTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theHeadingAndThePreviewAreNamed() {
        rule.setMonarchContent {
            MonarchPreJoinLobby(micOn = true, onMicOnChange = {}, onJoin = { _, _ -> }, icon = lobbyGlyph)
        }
        rule.onNodeWithText("Join the call").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithContentDescription("Your camera preview").assert(
            SemanticsMatcher("reads Camera is off") { node ->
                node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { it.text == "Camera is off" }
            },
        )
    }

    @Test
    fun theTogglesAreHoistedAndJoinPassesTheChoice() {
        var mic by mutableStateOf(true)
        var camera by mutableStateOf(false)
        val joins = mutableListOf<Pair<Boolean, Boolean>>()
        rule.setMonarchContent {
            MonarchPreJoinLobby(
                micOn = mic,
                onMicOnChange = { mic = it },
                onJoin = { m, c -> joins += m to c },
                icon = lobbyGlyph,
                cameraOn = camera,
                onCameraOnChange = { camera = it },
            )
        }
        val microphone = rule.onNodeWithContentDescription("Microphone")
        microphone.assertIsOn().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(false, mic)
        microphone.assertIsOff()
        rule.onNodeWithContentDescription("Camera").assertIsOff().performClick()
        assertEquals(true, camera)
        rule.onNodeWithText("Join now").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(false to true), joins)
    }

    @Test
    fun theMeterIsNamedAndSaysMutedWhileTheMicrophoneIsOff() {
        var mic by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchPreJoinLobby(
                micOn = mic,
                onMicOnChange = {},
                onJoin = { _, _ -> },
                icon = lobbyGlyph,
                audioLevel = 0.5f,
            )
        }
        assertEquals("50%", rule.onNodeWithContentDescription("Microphone level").stateDescription())
        mic = false
        assertEquals("Muted", rule.onNodeWithContentDescription("Microphone level").stateDescription())
    }

    @Test
    fun withoutOnCameraOnChangeThereIsNoCameraToggle() {
        rule.setMonarchContent {
            MonarchPreJoinLobby(micOn = true, onMicOnChange = {}, onJoin = { _, _ -> }, icon = lobbyGlyph)
        }
        rule.onAllNodesWithContentDescription("Camera").assertCountEquals(0)
        rule.onAllNodesWithText("Cancel").assertCountEquals(0)
    }

    @Test
    fun theAppPreviewShowsOnlyWhileTheCameraIsOn() {
        var camera by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchPreJoinLobby(
                micOn = true,
                onMicOnChange = {},
                onJoin = { _, _ -> },
                icon = lobbyGlyph,
                cameraOn = camera,
                onCameraOnChange = { camera = it },
            ) { Box(Modifier.fillMaxSize().testTag("video")) }
        }
        rule.onNodeWithTag("video", useUnmergedTree = true).assertExists()
        camera = false
        rule.onAllNodesWithText("Camera is off", useUnmergedTree = true).assertCountEquals(1)
        rule.onNodeWithTag("video", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun whileJoiningTheControlsAreOffAndCancelCannotBePressed() {
        var cancelled = 0
        rule.setMonarchContent {
            MonarchPreJoinLobby(
                micOn = true,
                onMicOnChange = {},
                onJoin = { _, _ -> },
                icon = lobbyGlyph,
                joining = true,
                onCancel = { cancelled++ },
            )
        }
        rule.onNodeWithContentDescription("Microphone").assertIsNotEnabled()
        rule.onNodeWithText("Cancel").assertIsNotEnabled()
        assertEquals("Joining…", rule.onNodeWithText("Join now").stateDescription())
        assertEquals(0, cancelled)
    }

    @Test
    fun anErrorIsShownAndThePreviewSaysThereIsNone() {
        rule.setMonarchContent {
            MonarchPreJoinLobby(
                micOn = true,
                onMicOnChange = {},
                onJoin = { _, _ -> },
                icon = lobbyGlyph,
                cameraOn = true,
                error = "Camera access is blocked.",
            )
        }
        rule.onNodeWithText("Camera access is blocked.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("No preview", useUnmergedTree = true).assertExists()
    }

    @Test
    fun thePeopleInTheCallAreCounted() {
        rule.setMonarchContent {
            MonarchPreJoinLobby(
                micOn = true,
                onMicOnChange = {},
                onJoin = { _, _ -> },
                icon = lobbyGlyph,
                participants = lobbyPeople,
            )
        }
        rule.onNodeWithText("7 already in the call", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("People in the call", substring = true).assertExists()
    }

    @Test
    fun thePreviewRuleFollowsTheCameraAndTheError() {
        val t = MonarchPreJoinLobbyTokens
        val p = MonarchPreJoinLobbyTokens.Preview.entries.associateBy { it.name }
        assertEquals(p["CameraOff"], t.preview(cameraOn = false, hasVideo = true, hasError = false))
        assertEquals(p["Video"], t.preview(cameraOn = true, hasVideo = true, hasError = false))
        assertEquals(p["Starting"], t.preview(cameraOn = true, hasVideo = false, hasError = false))
        assertEquals(p["Unavailable"], t.preview(cameraOn = true, hasVideo = true, hasError = true))
    }

    @Test
    fun cancelComesBeforeJoinInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchPreJoinLobby(
                micOn = true,
                onMicOnChange = {},
                onJoin = { _, _ -> },
                icon = lobbyGlyph,
                onCancel = {},
            )
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Cancel").assertIsBefore(rule.onNodeWithText("Join now"), direction)
        }
    }

    @Test
    fun textWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            // A lobby is taller than the test window at this size; apps place it in a scrolling screen.
            Column(Modifier.verticalScroll(rememberScrollState())) {
                MonarchPreJoinLobby(
                    micOn = true,
                    onMicOnChange = {},
                    onJoin = { _, _ -> },
                    icon = lobbyGlyph,
                    joinLabel = "Start the weekly planning call",
                )
            }
        }
        rule.onNodeWithText("Join the call").assertTextFullyVisible()
        rule.onNodeWithText("Camera is off", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Start the weekly planning call", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchPreJoinLobby(
                micOn = true,
                onMicOnChange = {},
                onJoin = { _, _ -> },
                icon = lobbyGlyph,
                participants = lobbyPeople.take(2),
                labels = MonarchPreJoinLobbyLabels(
                    title = "Unirse",
                    cameraOff = "Cámara apagada",
                    alreadyIn = { "$it en la llamada" },
                    join = "Entrar",
                ),
            )
        }
        rule.onNodeWithText("Unirse").assertExists()
        rule.onNodeWithText("Cámara apagada", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("2 en la llamada", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Entrar").assertExists()
    }

    @Test
    fun theColoursComeFromTheirRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchPreJoinLobbyTokens
            assertEquals(c.bgMuted, t.previewBackground(c), theme.id)
            assertEquals(c.textMuted, t.placeholderColor(c), theme.id)
            assertEquals(c.textMuted, t.mutedColor(c), theme.id)
        }
    }
}
