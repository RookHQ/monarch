package com.rookhq.monarch.components.collaboration

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.collaboration.devicepicker.DevicePickerNotice
import com.rookhq.monarch.components.collaboration.devicepicker.MonarchDevicePicker
import com.rookhq.monarch.components.collaboration.devicepicker.MonarchDevicePickerAccess
import com.rookhq.monarch.components.collaboration.devicepicker.MonarchDevicePickerDevice
import com.rookhq.monarch.components.collaboration.devicepicker.MonarchDevicePickerLabels
import com.rookhq.monarch.components.collaboration.devicepicker.MonarchDevicePickerSize
import com.rookhq.monarch.components.collaboration.devicepicker.MonarchDevicePickerTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val mics = listOf(
    MonarchDevicePickerDevice("desk", "Desk microphone"),
    MonarchDevicePickerDevice("headset", "Headset microphone"),
)
private val cams = listOf(MonarchDevicePickerDevice("front", "Front camera"), MonarchDevicePickerDevice("back", ""))
private val outputs = listOf(
    MonarchDevicePickerDevice("earpiece", "Earpiece"),
    MonarchDevicePickerDevice("speaker", "Speaker phone"),
)

internal object MonarchDevicePickerFixtures : ComponentFixtures {
    override val id = "device-picker"
    override val fixtures = listOf(
        fixture("granted, microphone and camera") {
            MonarchDevicePicker(
                microphone = "desk",
                onMicrophoneChange = {},
                onCameraChange = {},
                microphones = mics,
                cameras = cams,
            )
        },
        fixture("small, with the audio output") {
            MonarchDevicePicker(
                onMicrophoneChange = {},
                speaker = "earpiece",
                onSpeakerChange = {},
                microphones = mics,
                speakers = outputs,
                size = MonarchDevicePickerSize.Sm,
            )
        },
        fixture("not asked, with Allow access") {
            MonarchDevicePicker(
                onMicrophoneChange = {},
                microphones = mics,
                access = MonarchDevicePickerAccess.NotDetermined,
                onRequestAccess = {},
            )
        },
        fixture("denied, with Open Settings") {
            MonarchDevicePicker(
                onCameraChange = {},
                cameras = cams,
                access = MonarchDevicePickerAccess.Denied,
                onOpenSettings = {},
            )
        },
        fixture("no devices") {
            MonarchDevicePicker(onMicrophoneChange = {}, onCameraChange = {})
        },
        fixture("disabled") {
            MonarchDevicePicker(
                onMicrophoneChange = {},
                microphones = mics,
                access = MonarchDevicePickerAccess.NotDetermined,
                enabled = false,
                onRequestAccess = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchDevicePickerTest {
    @get:Rule
    val rule = createComposeRule()

    private val live = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun aFieldIsShownForEachCallback() {
        rule.setMonarchContent {
            MonarchDevicePicker(onMicrophoneChange = {}, onCameraChange = {}, microphones = mics, cameras = cams)
        }
        rule.onNode(hasContentDescription("Microphone") and hasText("System default")).assertExists()
        rule.onNode(hasContentDescription("Camera") and hasText("System default")).assertExists()
        rule.onNodeWithText("Speaker").assertDoesNotExist()
    }

    @Test
    fun choosingADeviceReportsItsId() {
        var chosen: String? = "none"
        rule.setMonarchContent { MonarchDevicePicker(onMicrophoneChange = { chosen = it }, microphones = mics) }
        rule.onNode(hasContentDescription("Microphone") and hasText("System default"))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNode(hasText("Headset microphone") and hasClickAction()).performClick()
        assertEquals("headset", chosen)
    }

    @Test
    fun systemDefaultIsReportedAsNull() {
        var chosen: String? = "desk"
        rule.setMonarchContent {
            MonarchDevicePicker(microphone = "desk", onMicrophoneChange = { chosen = it }, microphones = mics)
        }
        rule.onNode(hasContentDescription("Microphone") and hasText("Desk microphone")).performClick()
        rule.onNode(hasText("System default") and hasClickAction()).performClick()
        assertNull(chosen)
    }

    @Test
    fun unnamedAndMissingDevicesAreNumbered() {
        rule.setMonarchContent {
            MonarchDevicePicker(camera = "usb", onCameraChange = {}, cameras = cams)
        }
        rule.onNode(hasContentDescription("Camera") and hasText("Camera 3")).performClick()
        rule.onNode(hasText("Camera 2") and hasClickAction()).assertExists()
        rule.onNode(hasText("Front camera") and hasClickAction()).assertExists()
    }

    @Test
    fun notAskedShowsAnInfoNoticeWithAllowAccess() {
        var asked = 0
        rule.setMonarchContent {
            MonarchDevicePicker(
                onMicrophoneChange = {},
                microphones = mics,
                access = MonarchDevicePickerAccess.NotDetermined,
                onRequestAccess = { asked++ },
            )
        }
        // The notice is its own polite live region; each field also keeps an (empty) one for its error.
        rule.onNode(live and hasText("Allow camera and microphone access to see device names.", substring = true))
            .assertExists()
        rule.onNode(hasText("Allow access") and hasClickAction())
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, asked)
        rule.onNode(hasContentDescription("Microphone") and hasText("System default")).assertIsNotEnabled()
    }

    @Test
    fun deniedShowsOpenSettings() {
        var opened = 0
        rule.setMonarchContent {
            MonarchDevicePicker(
                onCameraChange = {},
                cameras = cams,
                access = MonarchDevicePickerAccess.Denied,
                onOpenSettings = { opened++ },
            )
        }
        rule.onNode(hasText("Camera and microphone access is off.", substring = true)).assertExists()
        rule.onNode(hasText("Open Settings") and hasClickAction()).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun withoutAccessTheOutputStaysEnabled() {
        rule.setMonarchContent {
            MonarchDevicePicker(
                onMicrophoneChange = {},
                onSpeakerChange = {},
                speakers = outputs,
                access = MonarchDevicePickerAccess.Denied,
            )
        }
        rule.onNode(hasContentDescription("Speaker") and hasText("System default")).assertIsEnabled()
        rule.onNode(hasContentDescription("Microphone") and hasText("System default")).assertIsNotEnabled()
    }

    @Test
    fun grantedWithoutDevicesSaysNoneWasFound() {
        rule.setMonarchContent { MonarchDevicePicker(onMicrophoneChange = {}, onCameraChange = {}) }
        rule.onNode(hasText("No camera or microphone was found.", substring = true)).assertExists()
    }

    @Test
    fun disabledTurnsOffTheFieldsAndTheButton() {
        var asked = 0
        rule.setMonarchContent {
            MonarchDevicePicker(
                onSpeakerChange = {},
                onMicrophoneChange = {},
                microphones = mics,
                speakers = outputs,
                access = MonarchDevicePickerAccess.NotDetermined,
                enabled = false,
                onRequestAccess = { asked++ },
            )
        }
        rule.onNode(hasContentDescription("Speaker") and hasText("System default")).assertIsNotEnabled()
        rule.onNode(hasText("Allow access") and hasClickAction()).assertIsNotEnabled().performClick()
        assertEquals(0, asked)
    }

    @Test
    fun theNoticeIsFullyVisibleAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchDevicePicker(
                onMicrophoneChange = {},
                microphones = mics,
                access = MonarchDevicePickerAccess.NotDetermined,
            )
        }
        rule.onNode(hasText("Allow camera and microphone access to see device names.", substring = true))
            .assertTextFullyVisible()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchDevicePicker(
                camera = "usb",
                onMicrophoneChange = {},
                onCameraChange = {},
                microphones = mics,
                cameras = cams,
                access = MonarchDevicePickerAccess.NotDetermined,
                onRequestAccess = {},
                labels = MonarchDevicePickerLabels(
                    microphone = "Mikrofon",
                    camera = "Kamera",
                    systemDefault = "Standard",
                    unnamedCamera = { "Kamera Nr. $it" },
                    notDetermined = "Zugriff erlauben, um Geräte zu wählen.",
                    allowAccess = "Erlauben",
                ),
            )
        }
        rule.onNode(hasContentDescription("Mikrofon") and hasText("Standard")).assertExists()
        rule.onNode(hasContentDescription("Kamera") and hasText("Kamera Nr. 3")).assertExists()
        rule.onNode(hasText("Zugriff erlauben", substring = true)).assertExists()
        rule.onNode(hasText("Erlauben") and hasClickAction()).assertExists()
    }

    @Test
    fun theOptionsAndNoticesArePure() {
        val tokens = MonarchDevicePickerTokens
        val options = tokens.options(
            listOf(MonarchDevicePickerDevice("", "Ghost")) + cams + MonarchDevicePickerDevice("front", "Again"),
            selected = "gone",
            systemDefault = "System default",
            unnamed = { "Camera $it" },
        )
        assertEquals(listOf("", "front", "back", "gone"), options.map { it.value })
        assertEquals(listOf("System default", "Front camera", "Camera 2", "Camera 3"), options.map { it.label })
        assertEquals("", tokens.selection(null))
        assertEquals("", tokens.selection(""))
        assertEquals("desk", tokens.selection("desk"))
        assertNull(tokens.choice(""))
        assertEquals("desk", tokens.choice("desk"))
        val access = MonarchDevicePickerAccess.Granted
        assertNull(tokens.notice(access, inputsShown = true, anyInput = true))
        assertNull(tokens.notice(access, inputsShown = false, anyInput = false))
        assertEquals(DevicePickerNotice.Unavailable, tokens.notice(access, inputsShown = true, anyInput = false))
        assertEquals(
            DevicePickerNotice.Denied,
            tokens.notice(MonarchDevicePickerAccess.Denied, inputsShown = true, anyInput = true),
        )
        assertEquals(MonarchTone.Info, tokens.tone(DevicePickerNotice.NotDetermined))
        assertEquals(MonarchTone.Warning, tokens.tone(DevicePickerNotice.Denied))
        assertEquals(MonarchTone.Neutral, tokens.tone(DevicePickerNotice.Unavailable))
        assertEquals(MonarchButtonSize.Sm, tokens.buttonSize(MonarchDevicePickerSize.Sm))
        assertEquals(MonarchButtonSize.Md, tokens.buttonSize(MonarchDevicePickerSize.Md))
        assertTrue(tokens.gap(MonarchDevicePickerSize.Sm) < tokens.gap(MonarchDevicePickerSize.Md))
        assertEquals(false, tokens.inputsEnabled(MonarchDevicePickerAccess.NotDetermined, enabled = true))
        assertEquals(true, tokens.inputsEnabled(access, enabled = true))
    }
}
