package dev.monarchui.components.layout.deviceframe

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/**
 * The drawing of one device: its default screen size, its bezel and its
 * outer corner radius.
 */
internal class DeviceFrameGeometry(val screen: DpSize, val bezel: Dp, val corner: Dp) {
    /** The corner radius of the screen inside the bezel. */
    val screenCorner: Dp get() = (corner - bezel).coerceAtLeast(Dp(0f))
}

/** The colours of a device frame. */
internal class DeviceFrameColors(val frame: Color, val screen: Color, val content: Color)

/**
 * Component tokens of MonarchDeviceFrame. The device dimensions follow the
 * web component, which draws a 390 by 844 phone and an 820 by 1180 tablet;
 * the bezels and corners are steps of the space scale.
 */
internal object MonarchDeviceFrameTokens {
    private const val PHONE_WIDTH = 390
    private const val PHONE_HEIGHT = 844
    private const val TABLET_WIDTH = 820
    private const val TABLET_HEIGHT = 1180

    /** The phone: a 10dp bezel and 44dp corners, as on the web. */
    val Phone: DeviceFrameGeometry = DeviceFrameGeometry(
        screen = DpSize(PHONE_WIDTH.dp, PHONE_HEIGHT.dp),
        bezel = MonarchTokens.Space.space2_5.dp,
        corner = (MonarchTokens.Space.space10 + MonarchTokens.Space.space1).dp,
    )

    /** The tablet: a 14dp bezel and 28dp corners, as on the web. */
    val Tablet: DeviceFrameGeometry = DeviceFrameGeometry(
        screen = DpSize(TABLET_WIDTH.dp, TABLET_HEIGHT.dp),
        bezel = (MonarchTokens.Space.space3 + MonarchTokens.Space.space0_5).dp,
        corner = (MonarchTokens.Space.space6 + MonarchTokens.Space.space1).dp,
    )

    /** The camera island of the phone: 30% of the screen width, at most 120dp, 22dp high, 8dp from the top. */
    const val ISLAND_WIDTH_FRACTION: Float = 0.3f
    val IslandMaxWidth: Dp = (MonarchTokens.Space.space24 + MonarchTokens.Space.space6).dp
    val IslandHeight: Dp = (MonarchTokens.Space.space5 + MonarchTokens.Space.space0_5).dp
    val IslandTop: Dp = MonarchTokens.Space.space2.dp

    /** The status bar: at least 40dp high, 24dp from the screen edges. */
    val StatusBarMinHeight: Dp = MonarchTokens.Space.space10.dp
    val StatusBarHorizontalPadding: Dp = MonarchTokens.Space.space6.dp

    /** The signal bars of the status bar. */
    val SignalBarWidth: Dp = (MonarchTokens.Space.space0_5 + MonarchTokens.Space.px).dp
    val SignalBarGap: Dp = MonarchTokens.Space.space0_5.dp
    val SignalHeight: Dp = MonarchTokens.Space.space2_5.dp
    val SignalHeights: List<Float> = listOf(0.4f, 0.7f, 1f)

    /** The side buttons drawn outside the frame: their width and their (top, height) as fractions of the frame. */
    val SideButtonWidth: Dp = (MonarchTokens.Space.space0_5 + MonarchTokens.Space.px).dp
    val StartButtons: List<Pair<Float, Float>> = listOf(0.14f to 0.04f, 0.21f to 0.07f, 0.30f to 0.07f)
    val EndButton: Pair<Float, Float> = 0.22f to 0.10f

    /** The geometry of a device. */
    fun geometry(device: MonarchDeviceFrameDevice): DeviceFrameGeometry = when (device) {
        MonarchDeviceFrameDevice.Phone -> Phone
        MonarchDeviceFrameDevice.Tablet -> Tablet
    }

    /** The volume buttons on the start edge are drawn on a phone only; the power button on both. */
    fun drawsStartButtons(device: MonarchDeviceFrameDevice): Boolean = device == MonarchDeviceFrameDevice.Phone

    /** The camera island is drawn on a phone only. */
    fun drawsIsland(device: MonarchDeviceFrameDevice): Boolean = device == MonarchDeviceFrameDevice.Phone

    /**
     * The colours: a dark neutral bezel in every theme (the inverse background in a light theme, the emphasis
     * background in a dark one, so a device is never a white slab), for the frame, the island and the buttons;
     * the surface and default text inside the screen.
     */
    fun colors(colors: MonarchColors, dark: Boolean): DeviceFrameColors = DeviceFrameColors(
        frame = if (dark) colors.bgEmphasis else colors.bgInverse,
        screen = colors.bgSurface,
        content = colors.textDefault,
    )
}
