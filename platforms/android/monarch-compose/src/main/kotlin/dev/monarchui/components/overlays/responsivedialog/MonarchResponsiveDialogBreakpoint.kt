package dev.monarchui.components.overlays.responsivedialog

/**
 * The window width class below which a `MonarchResponsiveDialog` is a bottom
 * sheet. The web's `sheetBelow` uses its `sm` (640) and `md` (768) pixel
 * breakpoints; Android uses the window size classes of the Material guidance.
 */
public enum class MonarchResponsiveDialogBreakpoint {
    /** A sheet on compact windows, narrower than 600dp: phones held upright. The default. */
    Medium,

    /** A sheet on compact and medium windows, narrower than 840dp: also small tablets and unfolded foldables. */
    Expanded,
}
