'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type KeyboardEvent,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
  type SVGProps,
  type WheelEvent as ReactWheelEvent,
} from 'react';
import { cx } from '../../../internal/cx.js';
import {
  BotGlyph,
  ChevronDownGlyph,
  ClockGlyph,
  DotGlyph,
  ErrorGlyph,
  EyeGlyph,
  SuccessGlyph,
  WarningGlyph,
} from '../../../internal/icons.js';
import { isSafeImageSrc } from '../../../internal/sanitize.js';
import { useControllable } from '../../../internal/use-controllable.js';
import { Button } from '../../actions/button/index.js';
import { Spinner } from '../../feedback/spinner/index.js';

/** Where the agent's session is. */
export type AiComputerViewStatus =
  | 'starting'
  | 'working'
  | 'paused'
  | 'waiting-for-you'
  | 'taken-over'
  | 'done'
  | 'failed'
  | 'stopped';

/** What kind of thing the agent did. */
export type AiComputerActionKind =
  'click' | 'type' | 'scroll' | 'navigate' | 'key' | 'wait' | 'read' | 'other';

/** What the agent is operating; only changes the wording. */
export type AiComputerViewSurface = 'browser' | 'computer';

/** A position on the screenshot, in frame pixels from its top-left corner. */
export interface AiComputerPoint {
  /** Pixels from the left edge of the frame. */
  x: number;
  /** Pixels from the top edge of the frame. */
  y: number;
}

/** The latest screenshot. */
export interface AiComputerFrame {
  /** Image URL. Rendered only when it is http(s), a same-origin path or a `blob:` URL. */
  src: string;
  /** Width of the screenshot in pixels; with `height` it reserves the box. */
  width: number;
  /** Height of the screenshot in pixels. */
  height: number;
  /** When the screenshot was taken, as an ISO 8601 string. */
  capturedAt: string;
  /** What the screenshot shows, in words. */
  alt?: string | undefined;
  /** The screenshot may show private information: it is never displayed. */
  sensitive?: boolean | undefined;
}

/** One thing the agent did. */
export interface AiComputerAction {
  /** Stable id: the React key and the value given to `onSelectAction`. */
  id: string;
  /** What kind of action it was; picks the icon. */
  kind: AiComputerActionKind;
  /** What happened, in plain words, e.g. "Clicked Sign in". Never put typed text here. */
  label: string;
  /** When it happened, as an ISO 8601 string. */
  at: string;
  /** Where on the frame it happened, in frame pixels. */
  point?: AiComputerPoint | undefined;
  /** More about it, e.g. the text typed or the address opened. */
  detail?: string | undefined;
  /**
   * The action touched something private: its detail and label are never
   * shown or announced, and the screenshot taken after it is hidden.
   */
  sensitive?: boolean | undefined;
}

/** The modifier keys held during a forwarded pointer or key input. */
export interface AiComputerInputModifiers {
  alt: boolean;
  ctrl: boolean;
  meta: boolean;
  shift: boolean;
}

/** A pointer input the person made on the screen while in control. */
export interface AiComputerPointerInput {
  /** What happened: the pointer moved, a button went down or up, or the wheel turned. */
  type: 'move' | 'down' | 'up' | 'wheel';
  /** Pixels from the left edge of the frame (or of the live picture). */
  x: number;
  /** Pixels from the top edge of the frame (or of the live picture). */
  y: number;
  /** The button that changed, as in `PointerEvent.button`; -1 for a move. */
  button: number;
  /** The buttons held, as in `PointerEvent.buttons`. */
  buttons: number;
  /** Horizontal wheel distance; 0 unless `type` is `wheel`. */
  deltaX: number;
  /** Vertical wheel distance; 0 unless `type` is `wheel`. */
  deltaY: number;
  /** The modifier keys held. */
  modifiers: AiComputerInputModifiers;
}

/** A key input the person made on the screen while in control. */
export interface AiComputerKeyInput {
  /** Whether the key went down or up. */
  type: 'down' | 'up';
  /** The key's value, as in `KeyboardEvent.key`. */
  key: string;
  /** The physical key, as in `KeyboardEvent.code`. */
  code: string;
  /** True when the key is held and repeating. */
  repeat: boolean;
  /** The modifier keys held. */
  modifiers: AiComputerInputModifiers;
}

/** The box of the screen on the page, as `getBoundingClientRect` gives it. */
export interface AiComputerViewBox {
  left: number;
  top: number;
  width: number;
  height: number;
}

/**
 * Converts a pointer position on the page (`clientX`, `clientY`) to frame
 * pixels, given the box the screen is drawn in and the frame's size. Positions
 * outside the box are clamped to its edge. Returns null when the box or the
 * size is not usable.
 */
export function aiComputerViewFramePoint(
  client: { clientX: number; clientY: number },
  box: AiComputerViewBox,
  size: Pick<AiComputerFrame, 'width' | 'height'>,
): AiComputerPoint | null {
  const values = [client.clientX, client.clientY, box.left, box.top, box.width, box.height];
  if (!values.every(Number.isFinite) || box.width <= 0 || box.height <= 0) return null;
  if (!(size.width > 0) || !(size.height > 0)) return null;
  const ratioX = Math.min(1, Math.max(0, (client.clientX - box.left) / box.width));
  const ratioY = Math.min(1, Math.max(0, (client.clientY - box.top) / box.height));
  return { x: Math.round(ratioX * size.width), y: Math.round(ratioY * size.height) };
}

/** A point as percentages of the frame, ready for `left` / `top`. */
export interface AiComputerViewPosition {
  /** Percent from the left edge, 0 to 100. */
  left: number;
  /** Percent from the top edge, 0 to 100. */
  top: number;
}

const round = (n: number) => Math.round(n * 100) / 100;
const clamp = (n: number) => Math.min(100, Math.max(0, n));

/**
 * Converts a point in frame pixels to percentages of the frame size, so a
 * marker stays on its target however the screenshot is scaled. Points outside
 * the frame are clamped to its edge. Returns null when the point or the frame
 * size is not usable.
 */
export function aiComputerViewPointPosition(
  point: AiComputerPoint | null | undefined,
  frame: Pick<AiComputerFrame, 'width' | 'height'> | null | undefined,
): AiComputerViewPosition | null {
  if (!point || !frame) return null;
  const { x, y } = point;
  const { width, height } = frame;
  if (![x, y, width, height].every(Number.isFinite) || width <= 0 || height <= 0) return null;
  return { left: round(clamp((x / width) * 100)), top: round(clamp((y / height) * 100)) };
}

/** What the log may show under an action's label. */
export interface AiComputerViewActionDetail {
  /** The detail text; undefined when there is none or it is withheld. */
  text: string | undefined;
  /** True when a detail exists (or the action is sensitive) and is withheld. */
  hidden: boolean;
}

/**
 * Decides whether an action's detail may be shown. A `sensitive` action never
 * shows it, whatever else is set; a `type` action shows it only when
 * `showTypedText` is true.
 */
export function aiComputerViewActionDetail(
  action: Pick<AiComputerAction, 'kind' | 'detail' | 'sensitive'>,
  showTypedText = false,
): AiComputerViewActionDetail {
  if (action.sensitive) return { text: undefined, hidden: true };
  if (!action.detail) return { text: undefined, hidden: false };
  if (action.kind === 'type' && !showTypedText) return { text: undefined, hidden: true };
  return { text: action.detail, hidden: false };
}

/** Every visible and assistive-technology string. English defaults. */
export interface AiComputerViewMessages {
  /** Accessible name of the whole region. */
  label: (surface: AiComputerViewSurface) => string;
  /** Status text shown beside the status icon. */
  statuses: Record<AiComputerViewStatus, string>;
  /** Banner while the agent is in control. */
  agentInControl: (surface: AiComputerViewSurface) => string;
  /** Banner while the person has taken over. */
  youInControl: (surface: AiComputerViewSurface) => string;
  /** Heading of the notice shown while the agent waits for the person. */
  waitingTitle: string;
  /** Reason shown when `waitingReason` is not given. */
  waitingFallback: string;
  /** Pause button. */
  pause: string;
  /** Resume button. */
  resume: string;
  /** Take over button. */
  takeOver: string;
  /** Hand back button. */
  handBack: string;
  /** Stop button. */
  stop: string;
  /** Question shown in the inline stop confirmation. */
  stopQuestion: string;
  /** Button that confirms stopping. */
  stopConfirm: string;
  /** Button that dismisses the stop confirmation. */
  stopCancel: string;
  /** Heading and accessible name of the action log. */
  logLabel: string;
  /** Disclosure button of the log in the compact variant, with the action count. */
  logToggle: (count: number) => string;
  /** Shown when there are no actions yet. */
  empty: string;
  /** Names of the action kinds, read before each label. */
  kinds: Record<AiComputerActionKind, string>;
  /** Shown in place of a withheld detail. */
  hidden: string;
  /** Shown in the frame box before the first screenshot arrives. */
  noFrame: string;
  /** Shown in the frame box when the screenshot URL is not allowed. */
  frameUnavailable: string;
  /** Text alternative used when the frame has no `alt`. */
  frameAlt: (surface: AiComputerViewSurface) => string;
  /** Joins the frame's text alternative with the latest action label. */
  frameDescription: (alt: string, latestAction: string | undefined) => string;
  /** Caption under the frame, from the formatted capture time. */
  capturedAt: (time: string) => string;
  /** Prefix read before the address. */
  addressLabel: string;
  /** Announced when one new action arrives. */
  announceAction: (label: string) => string;
  /** Announced when several actions arrived since the last announcement. */
  announceActions: (count: number, latestLabel: string) => string;
  /** Announced when the status changes. */
  announceStatus: (status: string) => string;
  /** Shown and announced in place of a sensitive action's label. */
  sensitiveLabel: string;
  /** Shown in the frame box when the screenshot may show private information. */
  frameHidden: string;
  /** Shows older actions when the log is windowed. */
  showEarlier: (count: number) => string;
  /** Text alternative of the live picture when the frame has no `alt`. */
  liveAlt: (surface: AiComputerViewSurface) => string;
  /** Caption under the live picture, in place of the capture time. */
  live: string;
  /** Accessible name of the screen while the person can act on it. */
  inputLabel: (surface: AiComputerViewSurface) => string;
  /** Shown under the screen while the person can act on it; names the key that leaves it. */
  inputHint: string;
  /** Announced when the person takes control. */
  announceTakenOver: (surface: AiComputerViewSurface) => string;
  /** Announced when control goes back to the agent. */
  announceReleased: (surface: AiComputerViewSurface) => string;
}

export const AI_COMPUTER_VIEW_MESSAGES: AiComputerViewMessages = {
  label: (surface) =>
    surface === 'computer' ? 'Assistant computer view' : 'Assistant browser view',
  statuses: {
    starting: 'Starting',
    working: 'Working',
    paused: 'Paused',
    'waiting-for-you': 'Waiting for you',
    'taken-over': 'You are in control',
    done: 'Done',
    failed: 'Failed',
    stopped: 'Stopped',
  },
  agentInControl: (surface) => `The assistant is controlling this ${surface}`,
  youInControl: (surface) => `You are controlling this ${surface}`,
  waitingTitle: 'The assistant needs you to continue',
  waitingFallback: 'This step is one the assistant must not do itself.',
  pause: 'Pause',
  resume: 'Resume',
  takeOver: 'Take over',
  handBack: 'Hand back',
  stop: 'Stop',
  stopQuestion: 'Stop the assistant? It will not finish the task.',
  stopConfirm: 'Stop now',
  stopCancel: 'Keep going',
  logLabel: 'Actions',
  logToggle: (count) => `Actions (${count})`,
  empty: 'No actions yet.',
  kinds: {
    click: 'Click',
    type: 'Type',
    scroll: 'Scroll',
    navigate: 'Navigate',
    key: 'Key press',
    wait: 'Wait',
    read: 'Read',
    other: 'Action',
  },
  hidden: 'Hidden',
  noFrame: 'No screenshot yet',
  frameUnavailable: 'Screenshot unavailable',
  frameAlt: (surface) => `Screenshot of the ${surface} the assistant is using`,
  frameDescription: (alt, latest) => (latest ? `${alt}. Latest action: ${latest}` : alt),
  capturedAt: (time) => `Screenshot from ${time}`,
  addressLabel: 'Address',
  announceAction: (label) => label,
  announceActions: (count, latest) => `${count} new actions. Latest: ${latest}`,
  announceStatus: (status) => `Assistant status: ${status}`,
  sensitiveLabel: 'Private action',
  frameHidden: 'Screenshot hidden: it may show private information',
  showEarlier: (count) => (count === 1 ? 'Show 1 earlier action' : `Show ${count} earlier actions`),
  liveAlt: (surface) => `Live view of the ${surface} the assistant is using`,
  live: 'Live',
  inputLabel: (surface) =>
    `The ${surface}. You are in control: your keys and pointer are sent to it`,
  inputHint: 'Your keys and pointer go to this screen. Press Shift+Escape to leave it.',
  announceTakenOver: (surface) =>
    `You are in control of this ${surface}. The assistant has stopped acting.`,
  announceReleased: (surface) => `The assistant is in control of this ${surface} again.`,
};

/** The label of an action as it may be shown: a sensitive one is never named. */
export function aiComputerViewActionLabel(
  action: Pick<AiComputerAction, 'label' | 'sensitive'>,
  sensitiveLabel: string = AI_COMPUTER_VIEW_MESSAGES.sensitiveLabel,
): string {
  return action.sensitive ? sensitiveLabel : action.label;
}

/** Ids remembered for announcements are pruned beyond this many. */
const MAX_SEEN = 2000;

export interface AiComputerViewProps extends Omit<
  HTMLAttributes<HTMLElement>,
  'children' | 'title'
> {
  /** The latest screenshot. Omit it until the first one arrives. */
  frame?: AiComputerFrame | null | undefined;
  /**
   * A live picture of the screen, e.g. the remote track of a WebRTC
   * connection. Shown in a muted `<video>` while it has a live video track and
   * hidden by the same privacy rules as a screenshot; when it ends or is
   * absent, the view falls back to `frame`. The product owns the stream and
   * stops its tracks.
   */
  stream?: MediaStream | null | undefined;
  /** The live picture may show private information: it is never displayed. */
  streamSensitive?: boolean | undefined;
  /** Where the session is. Each change is announced once. */
  status: AiComputerViewStatus;
  /** What the agent did, oldest first. New entries are announced politely, throttled. */
  actions?: readonly AiComputerAction[] | undefined;
  /** Address of the page the agent is on, shown as text in the window bar (never a link). */
  currentUrl?: string | undefined;
  /** Title of the window or page, shown in the window bar. */
  windowTitle?: string | undefined;
  /** Where the agent's pointer is now, in frame pixels. */
  pointer?: AiComputerPoint | null | undefined;
  /** What is being operated; only changes the wording. Default `browser`. */
  surface?: AiComputerViewSurface | undefined;
  /** Why the agent is waiting, e.g. "Sign in to continue". Shown while `waiting-for-you`. */
  waitingReason?: string | undefined;
  /** Shows Pause while working and Resume while paused; called with the wanted paused state. */
  onPauseChange?: ((paused: boolean) => void) | undefined;
  /** Shows Take over while the agent holds control; the product hands control to the person. */
  onTakeOver?: (() => void) | undefined;
  /** Shows Hand back while taken over; the product returns control to the agent. */
  onHandBack?: (() => void) | undefined;
  /**
   * Called when the person gives control back, with `onHandBack`. Either one
   * shows Hand back; pair it with `onTakeOver` as the take-over channel.
   */
  onRelease?: (() => void) | undefined;
  /**
   * Receives the person's pointer input on the screen, in frame pixels. Called
   * only while `status` is `taken-over` and the picture is shown.
   */
  onPointer?: ((input: AiComputerPointerInput) => void) | undefined;
  /**
   * Receives the person's key presses while the screen has focus. Called only
   * while `status` is `taken-over` and the picture is shown. Shift+Escape is
   * never forwarded: it moves focus out of the screen.
   */
  onKey?: ((input: AiComputerKeyInput) => void) | undefined;
  /** Shows Stop until the session has finished; called after the inline confirmation. */
  onStop?: (() => void) | undefined;
  /** The highlighted action (controlled). Its point is marked on the frame. */
  selectedActionId?: string | null | undefined;
  /** The highlighted action at first (uncontrolled). */
  defaultSelectedActionId?: string | null | undefined;
  /** Called with the action's id when the person selects it in the log. */
  onSelectAction?: ((id: string) => void) | undefined;
  /** Shows the detail of `type` actions. Sensitive actions stay hidden. Default false. */
  showTypedText?: boolean | undefined;
  /** Hides the log behind a disclosure and keeps it under the screenshot. */
  compact?: boolean | undefined;
  /** Whether the compact log is open (controlled). */
  logOpen?: boolean | undefined;
  /** Whether the compact log is open at first (uncontrolled). Default false. */
  defaultLogOpen?: boolean | undefined;
  /** Called when the person opens or closes the compact log. */
  onLogOpenChange?: ((open: boolean) => void) | undefined;
  /** Shortest time between two action announcements, in milliseconds. Default 4000. */
  announceInterval?: number | undefined;
  /**
   * How many of the newest actions the log renders; older ones are behind
   * "Show earlier". Keeps long sessions fast. Default 200.
   */
  visibleActions?: number | undefined;
  /** BCP 47 locale for the times. Default: the runtime locale. */
  locale?: string | undefined;
  /** IANA time zone for the times; pass it for identical server rendering. */
  timeZone?: string | undefined;
  /** Replaces any of the English strings; `statuses` and `kinds` may be partial. */
  messages?:
    | (Partial<Omit<AiComputerViewMessages, 'statuses' | 'kinds'>> & {
        statuses?: Partial<Record<AiComputerViewStatus, string>> | undefined;
        kinds?: Partial<Record<AiComputerActionKind, string>> | undefined;
      })
    | undefined;
}

const glyph = {
  width: 16,
  height: 16,
  viewBox: '0 0 16 16',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.5,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
  focusable: false,
} as const;

function Glyph({ children, ...props }: SVGProps<SVGSVGElement>) {
  return (
    <svg {...glyph} {...props} className={cx('mn-glyph', props.className)}>
      {children}
    </svg>
  );
}

const KIND_ICON: Record<AiComputerActionKind, ReactNode> = {
  click: (
    <Glyph>
      <path d="M4 2.5l8 5-3.5.9 2 3.6-1.5.8-2-3.6L4.5 12z" />
    </Glyph>
  ),
  type: (
    <Glyph>
      <rect x="1.5" y="4" width="13" height="8" rx="1.5" />
      <path d="M4 7h.01M6.5 7h.01M9 7h.01M11.5 7h.01M5 9.5h6" />
    </Glyph>
  ),
  scroll: (
    <Glyph>
      <path d="M8 2.5v11M5 5.5l3-3 3 3M5 10.5l3 3 3-3" />
    </Glyph>
  ),
  navigate: (
    <Glyph>
      <circle cx="8" cy="8" r="5.5" />
      <path d="M2.5 8h11M8 2.5c2 2 2 9 0 11M8 2.5c-2 2-2 9 0 11" />
    </Glyph>
  ),
  key: (
    <Glyph>
      <rect x="3" y="3" width="10" height="10" rx="2" />
      <path d="M6 10l2-4 2 4M6.75 8.75h2.5" />
    </Glyph>
  ),
  wait: <ClockGlyph />,
  read: <EyeGlyph />,
  other: <DotGlyph />,
};

const PauseGlyph = () => (
  <Glyph>
    <path d="M5.5 3.5v9M10.5 3.5v9" />
  </Glyph>
);
const PlayGlyph = () => (
  <Glyph>
    <path d="M5 3l7.5 5L5 13z" />
  </Glyph>
);
const StopGlyph = () => (
  <Glyph>
    <rect x="3.5" y="3.5" width="9" height="9" rx="1.5" />
  </Glyph>
);
const PersonGlyph = () => (
  <Glyph>
    <circle cx="8" cy="5.5" r="2.5" />
    <path d="M3 13.5c.5-2.5 2.5-4 5-4s4.5 1.5 5 4" />
  </Glyph>
);

const STATUS_ICON: Record<AiComputerViewStatus, ReactNode> = {
  starting: <Spinner size="sm" />,
  working: <BotGlyph />,
  paused: <PauseGlyph />,
  'waiting-for-you': <WarningGlyph />,
  'taken-over': <PersonGlyph />,
  done: <SuccessGlyph />,
  failed: <ErrorGlyph />,
  stopped: <StopGlyph />,
};

const FINISHED: readonly AiComputerViewStatus[] = ['done', 'failed', 'stopped'];
const AGENT_HOLDS: readonly AiComputerViewStatus[] = [
  'starting',
  'working',
  'paused',
  'waiting-for-you',
];
const AGENT_ACTS: readonly AiComputerViewStatus[] = ['starting', 'working'];
/** How close to the end (in pixels) still counts as "at the newest action". */
const STICK_THRESHOLD = 24;

/** The address without any `user:password@` part, so credentials never show. */
function displayUrl(url: string): string {
  return url.replace(/^([a-z][a-z0-9+.-]*:\/\/)[^/?#@]*@/i, '$1');
}

const position = (p: AiComputerViewPosition): CSSProperties => ({
  // Physical on purpose: frame pixels count from the left edge in every writing direction.
  left: `${p.left}%`,
  top: `${p.top}%`,
});

/**
 * A live view of an AI agent operating a browser or a computer for the person:
 * the latest screenshot with the agent's pointer and last action marked, a
 * plain-words status, a banner while the agent is in control, a log of every
 * action, and explicit buttons to pause, take over, hand back and stop. It is
 * a viewer: the product supplies frames and actions and performs all control.
 */
export const AiComputerView = forwardRef<HTMLElement, AiComputerViewProps>(function AiComputerView(
  {
    frame,
    status,
    actions = [],
    currentUrl,
    windowTitle,
    pointer,
    surface = 'browser',
    waitingReason,
    onPauseChange,
    onTakeOver,
    onHandBack,
    onRelease,
    onPointer,
    onKey,
    stream,
    streamSensitive = false,
    onStop,
    selectedActionId,
    defaultSelectedActionId = null,
    onSelectAction,
    showTypedText = false,
    compact = false,
    logOpen,
    defaultLogOpen = false,
    onLogOpenChange,
    announceInterval = 4000,
    visibleActions = 200,
    locale,
    timeZone,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const { statuses: _s, kinds: _k, ...overrides } = messagesProp ?? {};
  const m = { ...AI_COMPUTER_VIEW_MESSAGES, ...overrides };
  const statuses = { ...AI_COMPUTER_VIEW_MESSAGES.statuses, ...messagesProp?.statuses };
  const kinds = { ...AI_COMPUTER_VIEW_MESSAGES.kinds, ...messagesProp?.kinds };
  const latestMessages = useRef(m);
  latestMessages.current = m;

  const logId = useId();
  const logTitleId = useId();
  const questionId = useId();
  const hintId = useId();

  const [selected, setSelected] = useControllable<string | null>(
    selectedActionId,
    defaultSelectedActionId,
  );
  const [open, setOpen] = useControllable(logOpen, defaultLogOpen, onLogOpenChange);
  const logVisible = !compact || open;

  const timeFormat = useMemo(
    () =>
      new Intl.DateTimeFormat(locale, {
        hour: 'numeric',
        minute: '2-digit',
        second: '2-digit',
        ...(timeZone ? { timeZone } : {}),
      }),
    [locale, timeZone],
  );
  const formatTime = (iso: string) => {
    const date = new Date(iso);
    return Number.isNaN(date.getTime()) ? undefined : timeFormat.format(date);
  };

  const finished = FINISHED.includes(status);
  const agentActing = status === 'starting' || status === 'working';
  const latest = actions[actions.length - 1];
  const selectedAction = selected ? actions.find((a) => a.id === selected) : undefined;

  /* Frame and overlay. */
  /* Live picture: shown while the stream has a live video track, else the screenshot. */
  const videoRef = useRef<HTMLVideoElement>(null);
  const [streamLive, setStreamLive] = useState(false);
  const [videoSize, setVideoSize] = useState<{ width: number; height: number } | null>(null);
  useEffect(() => {
    if (!stream) {
      setStreamLive(false);
      return;
    }
    const check = () =>
      setStreamLive(stream.getVideoTracks().some((track) => track.readyState === 'live'));
    check();
    const tracks = stream.getVideoTracks();
    for (const track of tracks) track.addEventListener('ended', check);
    stream.addEventListener('addtrack', check);
    stream.addEventListener('removetrack', check);
    return () => {
      for (const track of tracks) track.removeEventListener('ended', check);
      stream.removeEventListener('addtrack', check);
      stream.removeEventListener('removetrack', check);
    };
  }, [stream]);
  const liveStream = stream && streamLive ? stream : undefined;

  const frameSized = frame && frame.width > 0 && frame.height > 0 ? frame : undefined;
  const sized = frameSized ?? (liveStream && videoSize ? videoSize : undefined);
  // A picture marked private, or taken right after a private action, is never shown.
  const frameHidden = Boolean(
    (frame || liveStream) &&
    (frame?.sensitive || latest?.sensitive || (liveStream && streamSensitive)),
  );
  const showStream = Boolean(liveStream) && !frameHidden;
  const safeSrc =
    frame && !showStream && !frameHidden && isSafeImageSrc(frame.src) ? frame.src : undefined;
  const pictureShown = showStream || Boolean(safeSrc);
  const description = m.frameDescription(
    frameHidden
      ? m.frameHidden
      : frame?.alt || (showStream ? m.liveAlt(surface) : m.frameAlt(surface)),
    latest ? aiComputerViewActionLabel(latest, m.sensitiveLabel) : undefined,
  );
  const captured = frame && !showStream ? formatTime(frame.capturedAt) : undefined;
  useEffect(() => {
    const video = videoRef.current;
    if (!video || !showStream || !liveStream) return;
    // Muted, inline and `autoPlay`: the picture starts without a gesture.
    video.srcObject = liveStream;
    return () => {
      // Detach only: the product owns the stream and stops its tracks.
      video.srcObject = null;
    };
  }, [liveStream, showStream]);
  let lastPointAction: AiComputerAction | undefined;
  for (let i = actions.length - 1; i >= 0 && !lastPointAction; i -= 1) {
    if (actions[i]?.point) lastPointAction = actions[i];
  }
  const selectedAt = aiComputerViewPointPosition(selectedAction?.point, sized);
  const lastAt =
    lastPointAction && lastPointAction.id !== selectedAction?.id
      ? aiComputerViewPointPosition(lastPointAction.point, sized)
      : null;
  const pointerAt = aiComputerViewPointPosition(pointer, sized);

  /* Stop confirmation: inline, focus moves in and back out. */
  const [confirming, setConfirming] = useState(false);
  const stopRef = useRef<HTMLButtonElement>(null);
  const confirmRef = useRef<HTMLButtonElement>(null);
  const returnFocus = useRef(false);
  useEffect(() => {
    if (confirming) confirmRef.current?.focus();
    else if (returnFocus.current) {
      returnFocus.current = false;
      stopRef.current?.focus();
    }
  }, [confirming]);
  useEffect(() => {
    if (finished) setConfirming(false);
  }, [finished]);
  const closeConfirm = () => {
    returnFocus.current = true;
    setConfirming(false);
  };

  /* Log: stay at the newest action unless the person scrolled up. */
  const scroller = useRef<HTMLDivElement>(null);
  const stick = useRef(true);
  useEffect(() => {
    const el = scroller.current;
    if (el && stick.current) el.scrollTop = el.scrollHeight;
  }, [actions.length, logVisible]);
  useEffect(() => {
    const el = scroller.current;
    if (!el) return;
    const onScroll = () => {
      stick.current = el.scrollHeight - el.scrollTop - el.clientHeight <= STICK_THRESHOLD;
    };
    el.addEventListener('scroll', onScroll, { passive: true });
    return () => el.removeEventListener('scroll', onScroll);
  }, []);

  /* Announcements: each status change once; new actions politely, throttled. */
  const [statusLive, setStatusLive] = useState<{ text: string; urgent: boolean }>({
    text: '',
    urgent: false,
  });
  const previousStatus = useRef<AiComputerViewStatus | null>(null);
  useEffect(() => {
    const before = previousStatus.current;
    previousStatus.current = status;
    if (before === null || before === status) return;
    const released = before === 'taken-over' && AGENT_ACTS.includes(status);
    setStatusLive({
      text:
        status === 'taken-over'
          ? m.announceTakenOver(surface)
          : released
            ? m.announceReleased(surface)
            : m.announceStatus(statuses[status]),
      urgent: status === 'failed' || status === 'waiting-for-you',
    });
    // Only a status change is news.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status]);

  const [actionLive, setActionLive] = useState('');
  const seen = useRef<Set<string> | null>(null);
  const pending = useRef({ count: 0, label: '' });
  const announcedAt = useRef(0);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  useEffect(() => {
    if (seen.current === null) {
      // What is there at first is history, not news.
      seen.current = new Set(actions.map((a) => a.id));
      return;
    }
    const known = seen.current;
    const fresh = actions.filter((a) => !known.has(a.id));
    if (!fresh.length) return;
    for (const a of fresh) known.add(a.id);
    // Bounded: ids that left the list are forgotten.
    if (known.size > Math.max(MAX_SEEN, actions.length * 2))
      seen.current = new Set(actions.map((a) => a.id));
    const newest = fresh[fresh.length - 1];
    pending.current = {
      count: pending.current.count + fresh.length,
      label: newest ? aiComputerViewActionLabel(newest, latestMessages.current.sensitiveLabel) : '',
    };
    const flush = () => {
      timer.current = null;
      const { count, label } = pending.current;
      if (!count) return;
      pending.current = { count: 0, label: '' };
      announcedAt.current = Date.now();
      const text = latestMessages.current;
      setActionLive(count === 1 ? text.announceAction(label) : text.announceActions(count, label));
    };
    const wait = announcedAt.current + announceInterval - Date.now();
    if (wait <= 0) flush();
    else if (timer.current === null) timer.current = setTimeout(flush, wait);
  }, [actions, announceInterval]);
  useEffect(
    () => () => {
      if (timer.current !== null) clearTimeout(timer.current);
    },
    [],
  );

  /* Long sessions: only the newest actions are rendered until asked. */
  const [showAll, setShowAll] = useState(false);
  const limit = Math.max(1, visibleActions);
  const hiddenBefore = showAll ? 0 : Math.max(0, actions.length - limit);
  const shownActions = hiddenBefore ? actions.slice(hiddenBefore) : actions;

  const canPause = Boolean(onPauseChange) && agentActing;
  const canResume = Boolean(onPauseChange) && status === 'paused';
  const canTakeOver = Boolean(onTakeOver) && AGENT_HOLDS.includes(status);
  const inControl = status === 'taken-over';
  const canHandBack = Boolean(onHandBack || onRelease) && inControl;
  // Input reaches the product only while the person is in control and can see the screen.
  const interactive = inControl && pictureShown && Boolean(onPointer || onKey);
  const canStop = Boolean(onStop) && !finished;
  const hasControls = canPause || canResume || canTakeOver || canHandBack || canStop;

  /* Control changes the person asked for: focus follows the button that replaced the one used. */
  const takeOverRef = useRef<HTMLButtonElement>(null);
  const handBackRef = useRef<HTMLButtonElement>(null);
  const surfaceRef = useRef<HTMLDivElement>(null);
  const controlFocus = useRef<'take' | 'release' | null>(null);
  useEffect(() => {
    const wanted = controlFocus.current;
    // The first status change after the click settles it, whichever way it went.
    controlFocus.current = null;
    if (wanted === 'take' && inControl) {
      (handBackRef.current ?? surfaceRef.current ?? stopRef.current)?.focus();
    } else if (wanted === 'release' && !inControl) {
      (takeOverRef.current ?? stopRef.current)?.focus();
    }
  }, [inControl, status]);

  const modifiers = (event: {
    altKey: boolean;
    ctrlKey: boolean;
    metaKey: boolean;
    shiftKey: boolean;
  }): AiComputerInputModifiers => ({
    alt: event.altKey,
    ctrl: event.ctrlKey,
    meta: event.metaKey,
    shift: event.shiftKey,
  });
  const forwardPointer = (
    type: AiComputerPointerInput['type'],
    event: ReactPointerEvent<HTMLDivElement> | ReactWheelEvent<HTMLDivElement>,
  ) => {
    if (!onPointer) return;
    const box = event.currentTarget.getBoundingClientRect();
    const point = aiComputerViewFramePoint(
      event,
      box,
      sized ?? { width: box.width, height: box.height },
    );
    if (!point) return;
    const deltas =
      'deltaY' in event ? { deltaX: event.deltaX, deltaY: event.deltaY } : { deltaX: 0, deltaY: 0 };
    onPointer({
      type,
      ...point,
      button: type === 'down' || type === 'up' ? event.button : -1,
      buttons: event.buttons,
      ...deltas,
      modifiers: modifiers(event),
    });
  };
  const forwardKey = (type: AiComputerKeyInput['type'], event: KeyboardEvent<HTMLDivElement>) => {
    if (event.target !== event.currentTarget) return;
    if (event.key === 'Escape' && event.shiftKey) {
      // The way out of the screen: never forwarded, so the keyboard is not trapped.
      event.preventDefault();
      if (type === 'down') (handBackRef.current ?? stopRef.current)?.focus();
      return;
    }
    if (!onKey) return;
    event.preventDefault();
    onKey({
      type,
      key: event.key,
      code: event.code,
      repeat: event.repeat,
      modifiers: modifiers(event),
    });
  };
  const surfaceProps = interactive
    ? ({
        ref: surfaceRef,
        role: 'application',
        tabIndex: 0,
        'aria-label': m.inputLabel(surface),
        'aria-describedby': hintId,
        'data-interactive': true,
        onPointerDown: (event: ReactPointerEvent<HTMLDivElement>) => {
          event.currentTarget.focus();
          forwardPointer('down', event);
        },
        onPointerMove: (event: ReactPointerEvent<HTMLDivElement>) => forwardPointer('move', event),
        onPointerUp: (event: ReactPointerEvent<HTMLDivElement>) => forwardPointer('up', event),
        onWheel: (event: ReactWheelEvent<HTMLDivElement>) => forwardPointer('wheel', event),
        onKeyDown: (event: KeyboardEvent<HTMLDivElement>) => forwardKey('down', event),
        onKeyUp: (event: KeyboardEvent<HTMLDivElement>) => forwardKey('up', event),
      } as const)
    : {};

  const onConfirmKeyDown = (event: KeyboardEvent<HTMLButtonElement>) => {
    if (event.key !== 'Escape') return;
    event.stopPropagation();
    closeConfirm();
  };

  return (
    <section
      ref={ref}
      // A named group, not a region: a page may show several, and landmarks must be unique.
      role="group"
      aria-label={m.label(surface)}
      {...rest}
      className={cx('mn-ai-computer-view', className)}
      data-status={status}
      data-compact={compact || undefined}
      data-controller={agentActing ? 'agent' : status === 'taken-over' ? 'you' : undefined}
      data-live={showStream || undefined}
    >
      <div className="mn-ai-computer-view__layout">
        <div className="mn-ai-computer-view__main">
          <div className="mn-ai-computer-view__header">
            <p className="mn-ai-computer-view__status">
              <span className="mn-ai-computer-view__status-icon" aria-hidden="true">
                {STATUS_ICON[status]}
              </span>
              {statuses[status]}
            </p>
            {agentActing || status === 'taken-over' ? (
              <p className="mn-ai-computer-view__banner">
                <span className="mn-ai-computer-view__banner-icon" aria-hidden="true">
                  {agentActing ? <BotGlyph /> : <PersonGlyph />}
                </span>
                {agentActing ? m.agentInControl(surface) : m.youInControl(surface)}
              </p>
            ) : null}
          </div>

          <figure className="mn-ai-computer-view__window">
            {windowTitle || currentUrl ? (
              <figcaption className="mn-ai-computer-view__chrome">
                {windowTitle ? (
                  <span className="mn-ai-computer-view__window-title">{windowTitle}</span>
                ) : null}
                {currentUrl ? (
                  <span className="mn-ai-computer-view__url">
                    <span className="mn-visually-hidden">{m.addressLabel}: </span>
                    <bdi dir="ltr">{displayUrl(currentUrl)}</bdi>
                  </span>
                ) : null}
              </figcaption>
            ) : null}
            <div
              {...surfaceProps}
              className="mn-ai-computer-view__frame"
              data-empty={pictureShown ? undefined : true}
              style={{ aspectRatio: sized ? `${sized.width} / ${sized.height}` : '16 / 10' }}
            >
              {showStream ? (
                <video
                  ref={videoRef}
                  className="mn-ai-computer-view__image mn-ai-computer-view__video"
                  aria-label={description}
                  autoPlay
                  muted
                  playsInline
                  disablePictureInPicture
                  onLoadedMetadata={(event) => {
                    const { videoWidth, videoHeight } = event.currentTarget;
                    if (videoWidth > 0 && videoHeight > 0)
                      setVideoSize({ width: videoWidth, height: videoHeight });
                  }}
                  onError={() => setStreamLive(false)}
                />
              ) : safeSrc ? (
                <img
                  className="mn-ai-computer-view__image"
                  src={safeSrc}
                  alt={description}
                  width={sized?.width}
                  height={sized?.height}
                  decoding="async"
                  draggable={false}
                />
              ) : (
                <p
                  className="mn-ai-computer-view__placeholder"
                  data-hidden={frameHidden || undefined}
                >
                  {frameHidden ? m.frameHidden : frame ? m.frameUnavailable : m.noFrame}
                  <span className="mn-visually-hidden">. {description}</span>
                </p>
              )}
              {pictureShown && (lastAt || selectedAt || pointerAt) ? (
                <div className="mn-ai-computer-view__overlay" aria-hidden="true">
                  {lastAt ? (
                    <span
                      className="mn-ai-computer-view__marker"
                      data-marker="last"
                      data-live={agentActing || undefined}
                      style={position(lastAt)}
                    />
                  ) : null}
                  {selectedAt ? (
                    <span
                      className="mn-ai-computer-view__marker"
                      data-marker="selected"
                      style={position(selectedAt)}
                    />
                  ) : null}
                  {pointerAt ? (
                    <span className="mn-ai-computer-view__pointer" style={position(pointerAt)}>
                      <Glyph>
                        <path d="M3 2l9.5 5.5-4 1.25L6.75 13z" />
                      </Glyph>
                    </span>
                  ) : null}
                </div>
              ) : null}
            </div>
            {showStream ? (
              <p className="mn-ai-computer-view__captured" data-live="true">
                <span className="mn-ai-computer-view__live-dot" aria-hidden="true" />
                {m.live}
              </p>
            ) : captured ? (
              <p className="mn-ai-computer-view__captured">
                <time dateTime={frame?.capturedAt}>{m.capturedAt(captured)}</time>
              </p>
            ) : null}
            {interactive ? (
              <p id={hintId} className="mn-ai-computer-view__input-hint">
                {m.inputHint}
              </p>
            ) : null}
          </figure>

          {status === 'waiting-for-you' ? (
            <div className="mn-ai-computer-view__waiting">
              <span className="mn-ai-computer-view__waiting-icon" aria-hidden="true">
                <WarningGlyph />
              </span>
              <div>
                <p className="mn-ai-computer-view__waiting-title">{m.waitingTitle}</p>
                <p className="mn-ai-computer-view__waiting-reason">
                  {waitingReason || m.waitingFallback}
                </p>
              </div>
            </div>
          ) : null}

          {hasControls ? (
            <div className="mn-ai-computer-view__controls">
              {confirming && canStop ? (
                <div
                  className="mn-ai-computer-view__confirm"
                  role="group"
                  aria-labelledby={questionId}
                >
                  <p id={questionId} className="mn-ai-computer-view__question">
                    {m.stopQuestion}
                  </p>
                  <div className="mn-ai-computer-view__confirm-actions">
                    <Button
                      ref={confirmRef}
                      size="sm"
                      variant="danger"
                      onKeyDown={onConfirmKeyDown}
                      onClick={() => {
                        closeConfirm();
                        onStop?.();
                      }}
                    >
                      {m.stopConfirm}
                    </Button>
                    <Button
                      size="sm"
                      variant="secondary"
                      onKeyDown={onConfirmKeyDown}
                      onClick={closeConfirm}
                    >
                      {m.stopCancel}
                    </Button>
                  </div>
                </div>
              ) : (
                <>
                  {canTakeOver ? (
                    <Button
                      ref={takeOverRef}
                      size="sm"
                      variant={status === 'waiting-for-you' ? 'primary' : 'secondary'}
                      leadingIcon={<PersonGlyph />}
                      onClick={() => {
                        controlFocus.current = 'take';
                        onTakeOver?.();
                      }}
                    >
                      {m.takeOver}
                    </Button>
                  ) : null}
                  {canHandBack ? (
                    <Button
                      ref={handBackRef}
                      size="sm"
                      variant="primary"
                      leadingIcon={<BotGlyph />}
                      onClick={() => {
                        controlFocus.current = 'release';
                        onHandBack?.();
                        onRelease?.();
                      }}
                    >
                      {m.handBack}
                    </Button>
                  ) : null}
                  {canPause ? (
                    <Button
                      size="sm"
                      variant="secondary"
                      leadingIcon={<PauseGlyph />}
                      onClick={() => onPauseChange?.(true)}
                    >
                      {m.pause}
                    </Button>
                  ) : null}
                  {canResume ? (
                    <Button
                      size="sm"
                      variant="secondary"
                      leadingIcon={<PlayGlyph />}
                      onClick={() => onPauseChange?.(false)}
                    >
                      {m.resume}
                    </Button>
                  ) : null}
                  {canStop ? (
                    <Button
                      ref={stopRef}
                      size="sm"
                      variant="danger-subtle"
                      className="mn-ai-computer-view__stop"
                      leadingIcon={<StopGlyph />}
                      onClick={() => setConfirming(true)}
                    >
                      {m.stop}
                    </Button>
                  ) : null}
                </>
              )}
            </div>
          ) : null}
        </div>

        <div className="mn-ai-computer-view__side">
          {compact ? (
            <button
              type="button"
              className="mn-ai-computer-view__toggle"
              aria-expanded={open}
              aria-controls={logId}
              onClick={() => setOpen(!open)}
            >
              <ChevronDownGlyph className="mn-ai-computer-view__chevron" />
              <span id={logTitleId}>{m.logToggle(actions.length)}</span>
            </button>
          ) : (
            <h3 id={logTitleId} className="mn-ai-computer-view__log-title">
              {m.logLabel}
            </h3>
          )}
          <div className="mn-ai-computer-view__log-box" hidden={!logVisible}>
            <div
              ref={scroller}
              id={logId}
              className="mn-ai-computer-view__log"
              role="log"
              // The throttled live region below speaks for the log.
              aria-live="off"
              aria-label={m.logLabel}
            >
              {actions.length === 0 ? (
                <p className="mn-ai-computer-view__empty">{m.empty}</p>
              ) : (
                <>
                  {hiddenBefore > 0 ? (
                    <button
                      type="button"
                      className="mn-ai-computer-view__earlier"
                      onClick={() => setShowAll(true)}
                    >
                      {m.showEarlier(hiddenBefore)}
                    </button>
                  ) : null}
                  <ol className="mn-ai-computer-view__actions" start={hiddenBefore + 1}>
                    {shownActions.map((action) => {
                      const detail = aiComputerViewActionDetail(action, showTypedText);
                      const time = formatTime(action.at);
                      const isSelected = action.id === selected;
                      return (
                        <li key={action.id} className="mn-ai-computer-view__item">
                          <button
                            type="button"
                            className="mn-ai-computer-view__action"
                            data-kind={action.kind}
                            data-selected={isSelected || undefined}
                            aria-pressed={isSelected}
                            onClick={() => {
                              setSelected(action.id);
                              onSelectAction?.(action.id);
                            }}
                          >
                            <span className="mn-ai-computer-view__kind" aria-hidden="true">
                              {KIND_ICON[action.kind] ?? KIND_ICON.other}
                            </span>
                            <span className="mn-ai-computer-view__text">
                              <span className="mn-ai-computer-view__label">
                                <span className="mn-visually-hidden">
                                  {kinds[action.kind] ?? kinds.other}:{' '}
                                </span>
                                {aiComputerViewActionLabel(action, m.sensitiveLabel)}
                              </span>
                              {detail.text ? (
                                <span className="mn-ai-computer-view__detail">{detail.text}</span>
                              ) : detail.hidden ? (
                                <span className="mn-ai-computer-view__detail" data-hidden="true">
                                  {m.hidden}
                                </span>
                              ) : null}
                            </span>
                            {time ? (
                              <time className="mn-ai-computer-view__time" dateTime={action.at}>
                                {time}
                              </time>
                            ) : null}
                          </button>
                        </li>
                      );
                    })}
                  </ol>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
        {statusLive.urgent ? '' : statusLive.text}
      </span>
      <span className="mn-visually-hidden" role="alert" aria-atomic="true">
        {statusLive.urgent ? statusLive.text : ''}
      </span>
      <span className="mn-visually-hidden" role="status" aria-live="polite" aria-atomic="true">
        {actionLive}
      </span>
    </section>
  );
});
