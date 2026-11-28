package dev.monarchui.components.actions.button

/**
 * The visual weight of a `MonarchButton`: the web variants, plus [Link].
 */
public enum class MonarchButtonVariant {
    /** The main action of a screen or a section. Filled with the accent. Use one per region. */
    Primary,

    /** Any other action. An outlined surface. The default. */
    Secondary,

    /** A low-emphasis action, for example in a toolbar or next to a primary button. No fill, no outline. */
    Ghost,

    /** An action that destroys data. Filled with the danger colour. */
    Danger,

    /** A quieter destructive action, for example Remove. A tinted fill with a danger outline. */
    DangerSubtle,

    /** An action that confirms a positive outcome, for example Approve. Filled with the success colour. */
    Success,

    /** An action that needs care, for example Mark at risk. Filled with the warning colour. */
    Warning,

    /** An action that reads as a link: the link colour and an underline, no fill. */
    Link,

    /**
     * A button on a dark surface in the light theme and a light surface in the dark theme: filled with the inverse
     * background and the inverse text. For controls over a call, a photo or a media bar.
     */
    Inverse,

    /** The AI action, such as Ask or Generate: filled with the AI colour. Use it only for what the assistant does. */
    Ai,
}
