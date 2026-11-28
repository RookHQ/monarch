package dev.monarchui.components.files

import android.content.Context
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import dev.monarchui.components.files.avatarupload.AvatarUploadContent
import dev.monarchui.components.files.avatarupload.AvatarUploadView
import dev.monarchui.components.files.avatarupload.MonarchAvatarCrop
import dev.monarchui.components.files.avatarupload.MonarchAvatarUpload
import dev.monarchui.components.files.avatarupload.MonarchAvatarUploadDecoder
import dev.monarchui.components.files.avatarupload.MonarchAvatarUploadLabels
import dev.monarchui.components.files.avatarupload.MonarchAvatarUploadResult
import dev.monarchui.components.files.avatarupload.MonarchAvatarUploadTokens
import dev.monarchui.components.files.fileupload.MonarchPickedFile
import dev.monarchui.components.identity.avatar.MonarchAvatarShape
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import dev.monarchui.theme.MonarchTheme
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val AvatarAuthority = "dev.monarchui.test.avatar"

private fun avatarFile(id: String): Uri = Uri.parse("content://$AvatarAuthority/$id")

/** A registry that answers every launch at once with [result]. */
private class AvatarRegistry(private val result: () -> Uri?) : ActivityResultRegistry() {
    var launches = 0

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        launches++
        dispatchResult(requestCode, result())
    }
}

@Composable
private fun AvatarUploadUnderTest(
    registry: ActivityResultRegistry,
    onChange: suspend (MonarchAvatarUploadResult) -> Unit = {},
    decode: (MonarchPickedFile) -> ImageBitmap? = { ImageBitmap(200, 100) },
    labels: MonarchAvatarUploadLabels = MonarchAvatarUploadLabels(),
) {
    val owner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry: ActivityResultRegistry = registry
    }
    CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
        AvatarUploadContent(
            name = "Ada Lovelace",
            image = null,
            onChange = onChange,
            modifier = Modifier,
            shape = MonarchAvatarShape.Circle,
            enabled = true,
            accept = listOf("image/png", "image/jpeg"),
            maxSize = null,
            outputSize = 64,
            onRemove = null,
            labels = labels,
            decode = decode,
            workContext = EmptyCoroutineContext,
        )
    }
}

internal object MonarchAvatarUploadFixtures : ComponentFixtures {
    override val id = "avatar-upload"
    override val fixtures = listOf(
        fixture("no picture") { MonarchAvatarUpload(name = "Ada Lovelace", image = null, onChange = {}) },
        fixture("picture with remove, square") {
            MonarchAvatarUpload(
                name = "Rook Labs",
                image = ColorPainter(MonarchTheme.colors.accentSolid),
                onChange = {},
                shape = MonarchAvatarShape.Square,
                onRemove = {},
            )
        },
        fixture("disabled") {
            MonarchAvatarUpload(name = "Ada Lovelace", image = null, onChange = {}, enabled = false)
        },
        fixture("long labels") {
            MonarchAvatarUpload(
                name = "Ada Lovelace",
                image = ColorPainter(MonarchTheme.colors.accentSolid),
                onChange = {},
                onRemove = {},
                labels = MonarchAvatarUploadLabels(
                    change = "Choose a different profile photo",
                    remove = "Remove the current profile photo",
                ),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAvatarUploadTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun registerProvider() {
        Robolectric.setupContentProvider(MonarchFileUploadTestProvider::class.java, AvatarAuthority)
    }

    @Test
    fun theButtonsFollowThePicture() {
        var removed = 0
        rule.setMonarchContent {
            Column {
                MonarchAvatarUpload(name = "Ada Lovelace", image = null, onChange = {})
                MonarchAvatarUpload(
                    name = "Grace Hopper",
                    image = ColorPainter(MonarchTheme.colors.accentSolid),
                    onChange = {},
                    onRemove = { removed++ },
                )
            }
        }
        rule.onNodeWithText("Upload photo").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Change photo").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Remove photo").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, removed)
        rule.onNode(hasContentDescription("Ada Lovelace")).assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aPickedPhotoOpensTheCropAreaWhichZoomsWithTalkBackActions() {
        val registry = AvatarRegistry { avatarFile("photo") }
        rule.setMonarchContent { AvatarUploadUnderTest(registry) }
        rule.onNodeWithText("Upload photo").performClick()
        rule.waitForIdle()
        val area = rule.onNodeWithContentDescription("Photo position")
        area.assertIsFocused()
        assertEquals("100%", area.stateDescription())
        area.performCustomAccessibilityActionWithLabel("Zoom in")
        assertEquals("110%", area.stateDescription())
        area.performCustomAccessibilityActionWithLabel("Zoom out")
        area.performCustomAccessibilityActionWithLabel("Zoom out")
        // Zoom stops at 100%.
        assertEquals("100%", area.stateDescription())
    }

    @Test
    fun saveHandsOverTheCroppedSquareAndReturnsFocus() {
        val registry = AvatarRegistry { avatarFile("photo") }
        var result: MonarchAvatarUploadResult? = null
        lateinit var inputMode: InputModeManager
        rule.setMonarchContent {
            inputMode = LocalInputModeManager.current
            AvatarUploadUnderTest(registry, onChange = { result = it })
        }
        // Buttons take focus with a keyboard, not in touch mode.
        rule.runOnIdle { inputMode.requestInputMode(InputMode.Keyboard) }
        rule.onNodeWithText("Upload photo").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Save photo").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        val saved = checkNotNull(result)
        assertEquals(64, saved.image.width)
        assertEquals(MonarchAvatarCrop(x = 50f, y = 0f, size = 100f), saved.crop)
        assertEquals("photo.png", saved.file.name)
        rule.onNodeWithContentDescription("Photo position").assertDoesNotExist()
        rule.onNodeWithText("Upload photo").assertIsFocused()
    }

    @Test
    fun aFailedSaveKeepsTheEditorAndSaysSo() {
        val registry = AvatarRegistry { avatarFile("photo") }
        rule.setMonarchContent { AvatarUploadUnderTest(registry, onChange = { throw IOException("Offline") }) }
        rule.onNodeWithText("Upload photo").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Save photo").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("The photo could not be saved. Try again.").assertExists()
        rule.onNodeWithContentDescription("Photo position").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun cancelAndEscapeLeaveWithoutSaving() {
        val registry = AvatarRegistry { avatarFile("photo") }
        var saves = 0
        rule.setMonarchContent { AvatarUploadUnderTest(registry, onChange = { saves++ }) }
        rule.onNodeWithText("Upload photo").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithContentDescription("Photo position").assertDoesNotExist()
        rule.onNodeWithText("Upload photo").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Photo position").performKeyInput { pressKey(Key.Escape) }
        rule.onNodeWithContentDescription("Photo position").assertDoesNotExist()
        assertEquals(0, saves)
    }

    @Test
    fun aRefusedOrUnreadableFileIsExplained() {
        var next = avatarFile("table")
        val registry = AvatarRegistry { next }
        rule.setMonarchContent { AvatarUploadUnderTest(registry, decode = { null }) }
        rule.onNodeWithText("Upload photo").performClick()
        rule.onNodeWithText("table.csv is not an allowed file type.").assertExists()
        next = avatarFile("photo")
        rule.onNodeWithText("Upload photo").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("The photo could not be opened.").assertExists()
        rule.onNodeWithContentDescription("Photo position").assertDoesNotExist()
    }

    @Test
    fun aDisabledUploadCannotOpenThePicker() {
        val registry = AvatarRegistry { avatarFile("photo") }
        rule.setMonarchContent {
            val owner = object : ActivityResultRegistryOwner {
                override val activityResultRegistry: ActivityResultRegistry = registry
            }
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                MonarchAvatarUpload(name = "Ada Lovelace", image = null, onChange = {}, enabled = false)
            }
        }
        rule.onNodeWithText("Upload photo").assertIsNotEnabled().performClick()
        assertEquals(0, registry.launches)
    }

    @Test
    fun theAvatarComesFirstRightToLeftAndLabelsWrap() {
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            MonarchAvatarUpload(
                name = "Ada Lovelace",
                image = null,
                onChange = {},
                labels = MonarchAvatarUploadLabels(upload = "Upload a profile photo"),
            )
        }
        rule.onNode(hasContentDescription("Ada Lovelace"), useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Upload a profile photo", useUnmergedTree = true), LayoutDirection.Rtl)
        rule.onNodeWithText("Upload a profile photo", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesOfTheCrop() {
        val t = MonarchAvatarUploadTokens
        assertEquals(MonarchAvatarCrop(50f, 0f, 100f), t.cropRect(200, 100, AvatarUploadView()))
        assertEquals(MonarchAvatarCrop(75f, 25f, 50f), t.cropRect(200, 100, AvatarUploadView(zoom = 2f)))
        // At zoom 1 a 2:1 image can move half a side to each side, and not up or down.
        assertEquals(AvatarUploadView(1f, 0.5f, 0f), t.clamp(200, 100, AvatarUploadView(1f, 3f, 3f)))
        assertEquals(4f, t.clamp(200, 100, AvatarUploadView(zoom = 9f)).zoom)
        assertEquals(1f, t.clamp(200, 100, AvatarUploadView(zoom = 0.2f)).zoom)
        assertEquals(150, t.percent(1.5f))
        // A 2:1 picture at zoom 1 fills the height of a 100 px area and overflows it by half on each side.
        assertEquals(Rect(-50f, 0f, 150f, 100f), t.imageBox(200, 100, AvatarUploadView(), 100f))
        assertEquals(Rect(-25f, 0f, 175f, 100f), t.imageBox(200, 100, AvatarUploadView(x = 0.25f), 100f))
        assertEquals(4, MonarchAvatarUploadDecoder.sampleSize(8000, 6000, 2048))
        assertEquals(1, MonarchAvatarUploadDecoder.sampleSize(800, 600, 2048))
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchAvatarUploadTokens
            assertEquals(c.bgCanvas, t.maskColor(c), theme.id)
            assertEquals(c.borderStrong, t.ringColor(c), theme.id)
            assertEquals(c.dangerStrong, t.errorColor(c), theme.id)
            assertEquals(c.textMuted, t.hintColor(c), theme.id)
        }
    }

    @Test
    fun theDecoderReturnsNothingForAFileThatIsNotAnImage() {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        assertNull(MonarchAvatarUploadDecoder.decode(resolver, avatarFile("missing"), 2048))
    }
}
