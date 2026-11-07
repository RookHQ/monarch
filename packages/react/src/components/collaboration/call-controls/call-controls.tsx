'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  type ButtonHTMLAttributes,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';

/* ------------------------------------------------------------------ icons */

/** Names of the built-in call glyphs. */
export type CallIconName =
  | 'mic'
  | 'mic-off'
  | 'camera'
  | 'camera-off'
  | 'screen'
  | 'screen-off'
  | 'hand'
  | 'smile'
  | 'more'
  | 'phone'
  | 'phone-off'
  | 'video'
  | 'expand'
  | 'shrink'
  | 'pin'
  | 'pin-off'
  | 'pip'
  | 'chevron-down'
  | 'settings'
  | 'activity'
  | 'grid'
  | 'speaker'
  | 'signal-low'
  | 'offline'
  | 'fullscreen'
  | 'fullscreen-exit';

const PATHS: Record<CallIconName, ReactNode> = {
  mic: (
    <>
      <rect x="9" y="2" width="6" height="12" rx="3" />
      <path d="M19 10v1a7 7 0 0 1-14 0v-1M12 18v4" />
    </>
  ),
  'mic-off': (
    <>
      <path d="M2 2l20 20M9 9v2a3 3 0 0 0 5.1 2.1M15 9.3V5a3 3 0 0 0-5.9-.7" />
      <path d="M17 16.9A7 7 0 0 1 5 11v-1M19 10v1a7 7 0 0 1-.1 1.2M12 18v4" />
    </>
  ),
  camera: (
    <>
      <path d="M16 13l5.2 3.5a.5.5 0 0 0 .8-.4V7.9a.5.5 0 0 0-.8-.4L16 11" />
      <rect x="2" y="6" width="14" height="12" rx="2" />
    </>
  ),
  'camera-off': (
    <>
      <path d="M10.7 6H14a2 2 0 0 1 2 2v3.3l1 1 5-3.4v8.2M16 16a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h2" />
      <path d="M2 2l20 20" />
    </>
  ),
  screen: (
    <>
      <rect x="2" y="3" width="20" height="14" rx="2" />
      <path d="M8 21h8M12 17v4M9 10l3-3 3 3M12 7v6" />
    </>
  ),
  'screen-off': (
    <>
      <rect x="2" y="3" width="20" height="14" rx="2" />
      <path d="M8 21h8M12 17v4M9.5 7.5l5 5M14.5 7.5l-5 5" />
    </>
  ),
  hand: (
    <path d="M18 11V6a2 2 0 0 0-4 0v0M14 10V4a2 2 0 0 0-4 0v2M10 10.5V6a2 2 0 0 0-4 0v8M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.9-6-2.4l-3.6-3.6a2 2 0 0 1 2.8-2.8L7 15" />
  ),
  smile: (
    <>
      <circle cx="12" cy="12" r="10" />
      <path d="M8 14s1.5 2 4 2 4-2 4-2M9 9h.01M15 9h.01" />
    </>
  ),
  more: (
    <>
      <circle cx="5" cy="12" r="1" />
      <circle cx="12" cy="12" r="1" />
      <circle cx="19" cy="12" r="1" />
    </>
  ),
  phone: (
    <path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1 1 .4 1.9.7 2.8a2 2 0 0 1-.5 2.1L8 9.9a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.3 1.8.6 2.8.7a2 2 0 0 1 1.7 2z" />
  ),
  'phone-off': (
    <>
      <path d="M10.7 13.3a16 16 0 0 1-2.7-3.4L9.3 8.6a2 2 0 0 0 .5-2.1 12.8 12.8 0 0 1-.7-2.8 2 2 0 0 0-2-1.7h-3a2 2 0 0 0-2 2.2 19.8 19.8 0 0 0 3.1 8.6 19.6 19.6 0 0 0 2.6 3.2" />
      <path d="M14.4 15.8l.9-.9a2 2 0 0 1 2.1-.5c.9.3 1.8.6 2.8.7a2 2 0 0 1 1.7 2v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1M22 2L2 22" />
    </>
  ),
  video: (
    <>
      <path d="M16 13l5.2 3.5a.5.5 0 0 0 .8-.4V7.9a.5.5 0 0 0-.8-.4L16 11" />
      <rect x="2" y="6" width="14" height="12" rx="2" />
    </>
  ),
  expand: <path d="M15 3h6v6M9 21H3v-6M21 3l-7 7M3 21l7-7" />,
  shrink: <path d="M4 14h6v6M20 10h-6V4M14 10l7-7M3 21l7-7" />,
  pin: (
    <path d="M12 17v5M9 10.8a2 2 0 0 1-1.1 1.8l-1.8.9A2 2 0 0 0 5 15.2V16a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-.8a2 2 0 0 0-1.1-1.7l-1.8-.9a2 2 0 0 1-1.1-1.8V7a1 1 0 0 1 1-1 2 2 0 0 0 0-4H8a2 2 0 0 0 0 4 1 1 0 0 1 1 1z" />
  ),
  'pin-off': (
    <path d="M12 17v5M15 9.3V7a1 1 0 0 1 1-1 2 2 0 0 0 0-4H7.9M2 2l20 20M9 9v1.8a2 2 0 0 1-1.1 1.8l-1.8.9A2 2 0 0 0 5 15.2V16a1 1 0 0 0 1 1h11" />
  ),
  pip: (
    <>
      <path d="M2 10V6a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-6" />
      <rect x="2" y="14" width="10" height="7" rx="1" />
    </>
  ),
  'chevron-down': <path d="M6 9l6 6 6-6" />,
  settings: (
    <>
      <path d="M20 7h-9M14 17H5" />
      <circle cx="17" cy="17" r="3" />
      <circle cx="7" cy="7" r="3" />
    </>
  ),
  activity: <path d="M22 12h-4l-3 9L9 3l-3 9H2" />,
  grid: (
    <>
      <rect x="3" y="3" width="7" height="7" rx="1" />
      <rect x="14" y="3" width="7" height="7" rx="1" />
      <rect x="3" y="14" width="7" height="7" rx="1" />
      <rect x="14" y="14" width="7" height="7" rx="1" />
    </>
  ),
  speaker: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21a8 8 0 0 1 16 0" />
    </>
  ),
  'signal-low': <path d="M2 20h.01M7 20v-4" />,
  offline: (
    <path d="M12 20h.01M8.5 16.4a5 5 0 0 1 7 0M5 12.9a10 10 0 0 1 5.2-2.8M19 12.9a10 10 0 0 0-2.3-1.7M2 8.8a15 15 0 0 1 4.2-2.6M22 8.8A15 15 0 0 0 11.1 5M2 2l20 20" />
  ),
  fullscreen: (
    <path d="M8 3H5a2 2 0 0 0-2 2v3M21 8V5a2 2 0 0 0-2-2h-3M3 16v3a2 2 0 0 0 2 2h3M16 21h3a2 2 0 0 0 2-2v-3" />
  ),
  'fullscreen-exit': (
    <path d="M8 3v3a2 2 0 0 1-2 2H3M21 8h-3a2 2 0 0 1-2-2V3M3 16h3a2 2 0 0 1 2 2v3M16 21v-3a2 2 0 0 1 2-2h3" />
  ),
};

export interface CallIconProps extends SVGProps<SVGSVGElement> {
  /** Which glyph to draw. */
  name: CallIconName;
}

/**
 * The call glyphs used by the call components (microphone, camera, screen,
 * hand, phone, layout and so on), drawn inline so Monarch needs no icon
 * package. Decorative: hidden from assistive technology; the control that
 * holds it carries the name.
 */
export function CallIcon({ name, className, ...rest }: CallIconProps) {
  return (
    <svg
      viewBox="0 0 24 24"
      width="20"
      height="20"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
      {...rest}
      className={cx('mn-call-icon', className)}
      data-icon={name}
    >
      {PATHS[name]}
    </svg>
  );
}

/* --------------------------------------------------------------- button */

/** Colour of a call control. */
export type CallControlTone = 'default' | 'off' | 'active' | 'raised' | 'danger';

export interface CallControlButtonProps extends Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  'children' | 'aria-label'
> {
  /** Accessible name, also shown as the tooltip. Required: the button shows only an icon. */
  label: string;
  /** The icon: a `CallIcon` or any 20px glyph. */
  icon: ReactNode;
  /** Keyboard shortcut appended to the tooltip, for example "M". */
  shortcut?: string | undefined;
  /**
   * Makes it a toggle (`aria-pressed`). Leave undefined for one-shot actions
   * such as Leave.
   */
  pressed?: boolean | undefined;
  /**
   * `off` (danger fill) marks a device that is switched off, `active`
   * (success fill) an action in progress such as screen sharing, `raised`
   * (warning fill) a raised hand, `danger` the Leave button. Default `default`.
   */
  tone?: CallControlTone | undefined;
  /** `md` is 44px (default), `sm` is 36px on fine pointers and 44px on touch. */
  size?: 'sm' | 'md' | undefined;
  /** Wider pill, used for the Leave button. */
  wide?: boolean | undefined;
}

/**
 * A round, icon-only call control: mute, camera, share, leave. Used by
 * CallControls, MiniCall, PreJoinLobby and IncomingCall, and available for
 * product-specific controls so they match.
 */
export const CallControlButton = forwardRef<HTMLButtonElement, CallControlButtonProps>(
  function CallControlButton(
    {
      label,
      icon,
      shortcut,
      pressed,
      tone = 'default',
      size = 'md',
      wide,
      className,
      type,
      ...rest
    },
    ref,
  ) {
    return (
      <button
        ref={ref}
        type={type ?? 'button'}
        {...rest}
        aria-label={label}
        aria-pressed={pressed}
        title={shortcut ? `${label} (${shortcut})` : label}
        data-tone={tone}
        data-size={size}
        data-wide={wide || undefined}
        className={cx('mn-call-control', className)}
      >
        {icon}
      </button>
    );
  },
);

/* ------------------------------------------------------------- toolbar */

/** How participants are arranged on the stage. */
export type CallViewMode = 'auto' | 'grid' | 'speaker';

/** Visible and announced strings. Override them to translate. */
export interface CallControlsMessages {
  toolbar: string;
  mute: string;
  unmute: string;
  muteShortcut: string;
  cameraOff: string;
  cameraOn: string;
  shareScreen: string;
  stopSharing: string;
  raiseHand: string;
  lowerHand: string;
  react: string;
  reactWith: (emoji: string) => string;
  more: string;
  layout: string;
  layouts: Record<CallViewMode, string>;
  lowData: string;
  devices: string;
  stats: string;
  fullscreen: string;
  exitFullscreen: string;
  pictureInPicture: string;
  leave: string;
}

export const DEFAULT_CALL_CONTROLS_MESSAGES: CallControlsMessages = {
  toolbar: 'Call controls',
  mute: 'Mute',
  unmute: 'Unmute',
  muteShortcut: 'M, or hold Space to talk',
  cameraOff: 'Turn camera off',
  cameraOn: 'Turn camera on',
  shareScreen: 'Share screen',
  stopSharing: 'Stop sharing',
  raiseHand: 'Raise hand',
  lowerHand: 'Lower hand',
  react: 'Send a reaction',
  reactWith: (emoji) => `React ${emoji}`,
  more: 'More options',
  layout: 'Layout',
  layouts: { auto: 'Automatic', grid: 'Grid', speaker: 'Speaker' },
  lowData: 'Low data mode (audio only)',
  devices: 'Devices',
  stats: 'Connection and data use',
  fullscreen: 'Full screen',
  exitFullscreen: 'Exit full screen',
  pictureInPicture: 'Picture-in-picture',
  leave: 'Leave call',
};

/** Default reactions offered by the reaction menu. */
export const DEFAULT_CALL_REACTIONS = ['👍', '👏', '😂', '❤️', '🎉', '😮'] as const;

export interface CallControlsProps extends HTMLAttributes<HTMLDivElement> {
  /** Microphone state. The mute button shows when `onMicChange` is set. */
  micOn?: boolean | undefined;
  onMicChange?: ((next: boolean) => void) | undefined;
  /** Camera state. The camera button shows when `onCameraChange` is set. */
  cameraOn?: boolean | undefined;
  onCameraChange?: ((next: boolean) => void) | undefined;
  /** Screen sharing state. The share button shows when `onSharingChange` is set. */
  sharing?: boolean | undefined;
  onSharingChange?: ((next: boolean) => void) | undefined;
  /** Raised hand state. The hand button shows when `onHandRaisedChange` is set. */
  handRaised?: boolean | undefined;
  onHandRaisedChange?: ((next: boolean) => void) | undefined;
  /** Sends a reaction. The reaction menu shows when this is set. */
  onReact?: ((emoji: string) => void) | undefined;
  /** Emoji offered in the reaction menu. Default `DEFAULT_CALL_REACTIONS`. */
  reactions?: readonly string[] | undefined;
  /** Current layout. The layout group in the More menu shows when `onModeChange` is set. */
  mode?: CallViewMode | undefined;
  onModeChange?: ((mode: CallViewMode) => void) | undefined;
  /** Audio-only mode. The More menu item shows when `onLowDataChange` is set. */
  lowData?: boolean | undefined;
  onLowDataChange?: ((next: boolean) => void) | undefined;
  /** Opens the product's device settings (for example a Popover with DevicePicker). */
  onOpenDevices?: (() => void) | undefined;
  /** Opens the product's connection panel (for example CallStatsPanel). */
  onOpenStats?: (() => void) | undefined;
  /** Full-screen state. The More menu item shows when `onFullscreenChange` is set. */
  fullscreen?: boolean | undefined;
  onFullscreenChange?: ((next: boolean) => void) | undefined;
  /** Opens picture-in-picture. The More menu item shows when set. */
  onPictureInPicture?: (() => void) | undefined;
  /** Leaves the call. The red Leave button shows when set. */
  onLeave?: (() => void) | undefined;
  /**
   * Hides the hand, reaction and More controls, for a small window such as
   * picture-in-picture where menus would open in the wrong window.
   */
  minimal?: boolean | undefined;
  /** Extra controls (CallControlButton) placed before Leave. */
  extra?: ReactNode;
  /** Override visible and announced strings. */
  messages?: Partial<CallControlsMessages> | undefined;
}

function focusSibling(event: ReactKeyboardEvent<HTMLDivElement>) {
  const keys = ['ArrowLeft', 'ArrowRight', 'Home', 'End'];
  if (!keys.includes(event.key)) return;
  const buttons = Array.from(
    event.currentTarget.querySelectorAll<HTMLButtonElement>('button:not(:disabled)'),
  );
  const index = buttons.indexOf(document.activeElement as HTMLButtonElement);
  if (index < 0) return;
  event.preventDefault();
  const last = buttons.length - 1;
  const next =
    event.key === 'Home'
      ? 0
      : event.key === 'End'
        ? last
        : event.key === 'ArrowRight'
          ? index === last
            ? 0
            : index + 1
          : index === 0
            ? last
            : index - 1;
  buttons[next]?.focus();
}

/**
 * The call toolbar: mute, camera, share, hand, reactions, a More menu (layout,
 * low data, devices, connection, full screen, picture-in-picture) and Leave.
 * Fully controlled: each control appears only when its callback is passed, and
 * the product decides what the call does.
 */
export const CallControls = forwardRef<HTMLDivElement, CallControlsProps>(function CallControls(
  {
    micOn = true,
    onMicChange,
    cameraOn = false,
    onCameraChange,
    sharing = false,
    onSharingChange,
    handRaised = false,
    onHandRaisedChange,
    onReact,
    reactions = DEFAULT_CALL_REACTIONS,
    mode = 'auto',
    onModeChange,
    lowData = false,
    onLowDataChange,
    onOpenDevices,
    onOpenStats,
    fullscreen = false,
    onFullscreenChange,
    onPictureInPicture,
    onLeave,
    minimal = false,
    extra,
    messages: messagesProp,
    className,
    onKeyDown,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_CALL_CONTROLS_MESSAGES, ...messagesProp };
  const hasMore =
    !minimal &&
    !!(
      onModeChange ||
      onLowDataChange ||
      onOpenDevices ||
      onOpenStats ||
      onFullscreenChange ||
      onPictureInPicture
    );
  return (
    <div
      ref={ref}
      role="toolbar"
      aria-label={m.toolbar}
      {...rest}
      className={cx('mn-call-controls', className)}
      onKeyDown={(event) => {
        onKeyDown?.(event);
        if (!event.defaultPrevented) focusSibling(event);
      }}
    >
      {onMicChange && (
        <CallControlButton
          label={micOn ? m.mute : m.unmute}
          shortcut={m.muteShortcut}
          pressed={!micOn}
          tone={micOn ? 'default' : 'off'}
          icon={<CallIcon name={micOn ? 'mic' : 'mic-off'} />}
          onClick={() => onMicChange(!micOn)}
        />
      )}
      {onCameraChange && (
        <CallControlButton
          label={cameraOn ? m.cameraOff : m.cameraOn}
          shortcut="V"
          pressed={!cameraOn}
          tone={cameraOn ? 'default' : 'off'}
          icon={<CallIcon name={cameraOn ? 'camera' : 'camera-off'} />}
          onClick={() => onCameraChange(!cameraOn)}
        />
      )}
      {onSharingChange && (
        <CallControlButton
          label={sharing ? m.stopSharing : m.shareScreen}
          shortcut="S"
          pressed={sharing}
          tone={sharing ? 'active' : 'default'}
          icon={<CallIcon name={sharing ? 'screen-off' : 'screen'} />}
          onClick={() => onSharingChange(!sharing)}
        />
      )}
      {!minimal && onHandRaisedChange && (
        <CallControlButton
          label={handRaised ? m.lowerHand : m.raiseHand}
          shortcut="H"
          pressed={handRaised}
          tone={handRaised ? 'raised' : 'default'}
          icon={<CallIcon name="hand" />}
          onClick={() => onHandRaisedChange(!handRaised)}
        />
      )}
      {!minimal && onReact && reactions.length > 0 && (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <CallControlButton label={m.react} icon={<CallIcon name="smile" />} />
          </DropdownMenuTrigger>
          <DropdownMenuContent side="top" sideOffset={8} className="mn-call-controls__reactions">
            {reactions.map((emoji) => (
              <DropdownMenuItem
                key={emoji}
                aria-label={m.reactWith(emoji)}
                className="mn-call-controls__reaction"
                onSelect={() => onReact(emoji)}
              >
                {emoji}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      )}
      {hasMore && (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <CallControlButton label={m.more} icon={<CallIcon name="more" />} />
          </DropdownMenuTrigger>
          <DropdownMenuContent side="top" sideOffset={8} align="end">
            {onModeChange && (
              <>
                <DropdownMenuLabel>{m.layout}</DropdownMenuLabel>
                <DropdownMenuRadioGroup
                  value={mode}
                  onValueChange={(value) => onModeChange(value as CallViewMode)}
                >
                  {(['auto', 'grid', 'speaker'] as const).map((value) => (
                    <DropdownMenuRadioItem key={value} value={value}>
                      {m.layouts[value]}
                    </DropdownMenuRadioItem>
                  ))}
                </DropdownMenuRadioGroup>
                <DropdownMenuSeparator />
              </>
            )}
            {onLowDataChange && (
              <DropdownMenuCheckboxItem
                checked={lowData}
                onCheckedChange={(checked) => onLowDataChange(checked === true)}
              >
                {m.lowData}
              </DropdownMenuCheckboxItem>
            )}
            {onOpenDevices && (
              <DropdownMenuItem icon={<CallIcon name="settings" />} onSelect={onOpenDevices}>
                {m.devices}
              </DropdownMenuItem>
            )}
            {onOpenStats && (
              <DropdownMenuItem icon={<CallIcon name="activity" />} onSelect={onOpenStats}>
                {m.stats}
              </DropdownMenuItem>
            )}
            {(onFullscreenChange || onPictureInPicture) &&
              (onModeChange || onLowDataChange || onOpenDevices || onOpenStats) && (
                <DropdownMenuSeparator />
              )}
            {onFullscreenChange && (
              <DropdownMenuItem
                icon={<CallIcon name={fullscreen ? 'fullscreen-exit' : 'fullscreen'} />}
                shortcut="F"
                onSelect={() => onFullscreenChange(!fullscreen)}
              >
                {fullscreen ? m.exitFullscreen : m.fullscreen}
              </DropdownMenuItem>
            )}
            {onPictureInPicture && (
              <DropdownMenuItem icon={<CallIcon name="pip" />} onSelect={onPictureInPicture}>
                {m.pictureInPicture}
              </DropdownMenuItem>
            )}
          </DropdownMenuContent>
        </DropdownMenu>
      )}
      {extra}
      {onLeave && (
        <CallControlButton
          label={m.leave}
          tone="danger"
          wide
          icon={<CallIcon name="phone-off" />}
          onClick={onLeave}
        />
      )}
    </div>
  );
});

/* ------------------------------------------------------------ shortcuts */

export interface CallShortcutHandlers {
  /** Turn the call shortcuts on. Default true. */
  enabled?: boolean | undefined;
  /**
   * The call's root element. Single-key shortcuts only fire when focus is
   * inside it, so they never take over typing elsewhere (WCAG 2.1.4).
   */
  root: () => HTMLElement | null;
  /** M, and Ctrl/Cmd+Shift+Space from anywhere. */
  onToggleMic?: (() => void) | undefined;
  /** Hold Space to talk: `true` on press, `false` on release or window blur. */
  onPushToTalk?: ((talking: boolean) => void) | undefined;
  /** V */
  onToggleCamera?: (() => void) | undefined;
  /** S */
  onToggleShare?: (() => void) | undefined;
  /** H */
  onToggleHand?: (() => void) | undefined;
  /** F */
  onToggleFullscreen?: (() => void) | undefined;
}

function isTyping(target: EventTarget | null) {
  const el = target as HTMLElement | null;
  return !!el && (el.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(el.tagName));
}

/**
 * Call keyboard shortcuts: M mic, V camera, S share, H hand, F full screen,
 * hold Space to talk, and Ctrl/Cmd+Shift+Space to toggle the mic from
 * anywhere. Single keys only fire while focus is inside `root()` and never
 * while typing in a field. Attaches listeners in an effect, so it is SSR-safe.
 */
export function useCallShortcuts(handlers: CallShortcutHandlers): void {
  const ref = useRef(handlers);
  ref.current = handlers;
  const enabled = handlers.enabled ?? true;
  useEffect(() => {
    if (!enabled || typeof window === 'undefined') return;
    const inCall = (target: EventTarget | null) => {
      const root = ref.current.root();
      return !!root && target instanceof Node && root.contains(target);
    };
    let talking = false;
    const down = (event: KeyboardEvent) => {
      const h = ref.current;
      if ((event.metaKey || event.ctrlKey) && event.shiftKey && event.code === 'Space') {
        if (!h.onToggleMic) return;
        event.preventDefault();
        h.onToggleMic();
        return;
      }
      if (event.metaKey || event.ctrlKey || event.altKey) return;
      if (isTyping(event.target) || !inCall(event.target)) return;
      if (event.code === 'Space') {
        // Space still activates a focused button; push-to-talk needs focus elsewhere.
        if (!h.onPushToTalk || (event.target as HTMLElement).closest?.('button, a, [role]')) {
          return;
        }
        event.preventDefault();
        if (!event.repeat && !talking) {
          talking = true;
          h.onPushToTalk(true);
        }
        return;
      }
      if (event.repeat) return;
      const key = event.key.toLowerCase();
      const action =
        key === 'm'
          ? h.onToggleMic
          : key === 'v'
            ? h.onToggleCamera
            : key === 's'
              ? h.onToggleShare
              : key === 'h'
                ? h.onToggleHand
                : key === 'f'
                  ? h.onToggleFullscreen
                  : undefined;
      if (!action) return;
      event.preventDefault();
      action();
    };
    const release = () => {
      if (!talking) return;
      talking = false;
      ref.current.onPushToTalk?.(false);
    };
    const up = (event: KeyboardEvent) => {
      if (event.code === 'Space') release();
    };
    window.addEventListener('keydown', down);
    window.addEventListener('keyup', up);
    window.addEventListener('blur', release);
    return () => {
      window.removeEventListener('keydown', down);
      window.removeEventListener('keyup', up);
      window.removeEventListener('blur', release);
    };
  }, [enabled]);
}
