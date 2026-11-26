import { useEffect, useState } from 'react';
import {
  AiComputerView,
  type AiComputerAction,
  type AiComputerFrame,
  type AiComputerViewStatus,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const WIDTH = 960;
const HEIGHT = 600;

/** A neutral stand-in screenshot as a local `blob:` URL, so nothing is hotlinked. */
function useDemoFrame(step: number): AiComputerFrame | null {
  const [src, setSrc] = useState<string | null>(null);
  useEffect(() => {
    if (typeof URL === 'undefined' || !URL.createObjectURL) return;
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${WIDTH} ${HEIGHT}"><rect width="${WIDTH}" height="${HEIGHT}" fill="white"/><rect width="${WIDTH}" height="64" fill-opacity="0.12"/><rect x="330" y="${150 + step * 12}" width="300" height="300" rx="12" fill-opacity="0.06"/><rect x="370" y="${210 + step * 12}" width="220" height="36" rx="6" fill-opacity="0.14"/><rect x="370" y="${270 + step * 12}" width="220" height="36" rx="6" fill-opacity="0.14"/><rect x="370" y="${350 + step * 12}" width="220" height="44" rx="8" fill-opacity="0.5"/></svg>`;
    const url = URL.createObjectURL(new Blob([svg], { type: 'image/svg+xml' }));
    setSrc(url);
    return () => URL.revokeObjectURL(url);
  }, [step]);
  return src
    ? {
        src,
        width: WIDTH,
        height: HEIGHT,
        capturedAt: '2026-01-01T12:00:09Z',
        alt: 'The sign-in page of the supplier portal',
      }
    : null;
}

const ACTIONS: AiComputerAction[] = [
  {
    id: 'a1',
    kind: 'navigate',
    label: 'Opened the supplier portal',
    at: '2026-01-01T12:00:01Z',
    detail: 'https://portal.example.com/sign-in',
  },
  {
    id: 'a2',
    kind: 'read',
    label: 'Read the sign-in form',
    at: '2026-01-01T12:00:03Z',
  },
  {
    id: 'a3',
    kind: 'type',
    label: 'Typed in the email field',
    at: '2026-01-01T12:00:05Z',
    point: { x: 480, y: 228 },
    detail: 'accounts@example.com',
  },
  {
    id: 'a4',
    kind: 'type',
    label: 'Typed in the password field',
    at: '2026-01-01T12:00:07Z',
    point: { x: 480, y: 288 },
    sensitive: true,
  },
  {
    id: 'a5',
    kind: 'click',
    label: 'Clicked Sign in',
    at: '2026-01-01T12:00:08Z',
    point: { x: 480, y: 372 },
  },
];

const shared = {
  windowTitle: 'Supplier portal',
  currentUrl: 'https://portal.example.com/sign-in',
  locale: 'en-GB',
  timeZone: 'UTC',
} as const;

function LiveDemo() {
  const [status, setStatus] = useState<AiComputerViewStatus>('working');
  const [count, setCount] = useState(2);
  const frame = useDemoFrame(0);
  useEffect(() => {
    if (status !== 'working') return;
    if (count >= ACTIONS.length) {
      setStatus('waiting-for-you');
      return;
    }
    const t = setTimeout(() => setCount((c) => c + 1), 1800);
    return () => clearTimeout(t);
  }, [status, count]);
  const actions = ACTIONS.slice(0, count);
  const last = actions[actions.length - 1];
  return (
    <AiComputerView
      {...shared}
      frame={frame}
      status={status}
      actions={actions}
      pointer={last?.point ?? null}
      waitingReason="The portal asks for a one-time code sent to your phone."
      onPauseChange={(paused) => setStatus(paused ? 'paused' : 'working')}
      onTakeOver={() => setStatus('taken-over')}
      onHandBack={() => {
        setCount(2);
        setStatus('working');
      }}
      onStop={() => setStatus('stopped')}
    />
  );
}

/** Take over, act on the screen, hand back. The last forwarded input is shown as text. */
function TakeOverDemo() {
  const [status, setStatus] = useState<AiComputerViewStatus>('working');
  const [last, setLast] = useState('Nothing sent yet.');
  const frame = useDemoFrame(0);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)' }}>
      <AiComputerView
        {...shared}
        frame={frame}
        status={status}
        actions={ACTIONS.slice(0, 3)}
        onTakeOver={() => setStatus('taken-over')}
        onRelease={() => setStatus('working')}
        onPointer={(input) => {
          if (input.type !== 'move') setLast(`Pointer ${input.type} at ${input.x}, ${input.y}`);
        }}
        onKey={(input) => {
          // A real product sends the key on; never show or log what was typed.
          if (input.type === 'down') setLast('Key sent');
        }}
        onStop={() => setStatus('stopped')}
      />
      <p style={{ margin: 0 }}>{last}</p>
    </div>
  );
}

function StaticDemo({
  status,
  compact,
  waitingReason,
}: {
  status: AiComputerViewStatus;
  compact?: boolean;
  waitingReason?: string;
}) {
  const frame = useDemoFrame(0);
  return (
    <AiComputerView
      {...shared}
      frame={frame}
      status={status}
      actions={ACTIONS}
      pointer={{ x: 520, y: 380 }}
      compact={compact}
      waitingReason={waitingReason}
      onPauseChange={() => {}}
      onTakeOver={() => {}}
      onHandBack={() => {}}
      onStop={() => {}}
    />
  );
}

export default {
  id: 'ai-computer-view',
  title: 'AiComputerView',
  summary:
    'A live view of an agent operating a browser or computer: screenshot or live stream, status, action log, pause, take over and stop.',
  sections: {
    purpose: {
      summary:
        'Lets a person watch an AI agent work in a browser or on a computer on their behalf: the latest screenshot with the pointer and last action marked, what it is doing in words, every action it took, and buttons to pause it, take over or stop it. It is a viewer: the product supplies the frames and the actions and performs all control.',
      useWhen: [
        'An agent clicks, types and navigates in a browser or desktop session for the person, and they must be able to see and interrupt it.',
        'A step may need the person (a sign-in, a payment, a one-time code) and you need a clear hand-over.',
        'Reviewing a finished session: the last frame with the log of what was done.',
      ],
      avoidWhen: [
        'A trace of tool calls with no screen to show: use AiAgentRunTimeline.',
        'A background task with only a progress bar and steps: use AiTaskProgress.',
        'An audit list of what agents changed in your own product: use AiActionLog.',
        'A general remote desktop client: input is forwarded only while the person has taken over from an agent, and the product carries it.',
      ],
    },
    designPrinciples: {
      items: [
        'Who is in control is never in doubt: a persistent banner in words while the assistant drives, another while the person does.',
        'Nothing is driven from here. Pause, Take over, Hand back and Stop are explicit buttons that call back; Stop asks once more inline and stays in view.',
        'Input goes to the screen only while the person is in control and can see it; the agent’s turn is never typed into by accident.',
        'Private things stay private: a sensitive action never shows its detail, and typed text is hidden unless the product opts in.',
        'Status is an icon plus words, never colour alone; the overlay markers are decoration and the log carries the same facts as text.',
        'The frame box is reserved from the screenshot size, so a new frame never moves the page.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<section>`; native attributes pass through.',
        '`aiComputerViewPointPosition(point, frame)` converts frame pixels to `{ left, top }` percentages (clamped to the frame); `aiComputerViewActionDetail(action, showTypedText)` applies the hidden-detail rules. Both are pure and exported for your own overlays or logs.',
        '`frame.src` is rendered only when it is http(s), a same-origin path or a `blob:` URL; anything else shows "Screenshot unavailable".',
        'Put what was typed in `detail`, never in `label`: the label is always shown and announced.',
        '`stream` is a `MediaStream` (for example the remote track of a WebRTC connection). It is shown in a muted `<video>` while it has a live video track; when it ends, errors or is removed the view falls back to `frame`. The component only attaches and detaches it: the product creates the stream and stops its tracks. `streamSensitive`, a `sensitive` frame or a `sensitive` latest action hide it exactly as they hide a screenshot.',
        'Take-over channel: `onTakeOver` asks for control and `onRelease` (called with `onHandBack`) gives it back; set `status` to `taken-over` once the product has really handed over. While it is, `onPointer` receives `{ type, x, y, button, buttons, deltaX, deltaY, modifiers }` in frame pixels and `onKey` receives `{ type, key, code, repeat, modifiers }`. Neither is called in any other status, or while the picture is hidden.',
        '`aiComputerViewFramePoint(client, box, size)` is the pure conversion from a page position to frame pixels, exported for your own surfaces.',
        'Privacy: forwarded keys can include passwords. Send them over the same protected channel as the session and do not log them.',
        'Pass `timeZone` (and `locale`) for identical server and client rendering of the times.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'compact',
          values: ['false', 'true'],
          description:
            'Compact keeps the log under the screenshot behind an "Actions (n)" disclosure.',
        },
        {
          prop: 'surface',
          values: ['browser', 'computer'],
          description: 'Changes the wording of the banner and the names only.',
        },
        {
          prop: 'status',
          values: [
            'starting',
            'working',
            'paused',
            'waiting-for-you',
            'taken-over',
            'done',
            'failed',
            'stopped',
          ],
          description:
            'Sets the status line, the banner, the waiting notice and which controls show.',
        },
      ],
      notes: ['Each control shows only when its callback is given and it can act.'],
    },
    states: {
      items: [
        { name: 'Status', attribute: 'data-status', description: 'The current status.' },
        {
          name: 'Controller',
          attribute: 'data-controller',
          description:
            '`agent` while starting or working (banner and accent frame), `you` while taken over; absent otherwise.',
        },
        {
          name: 'Compact',
          attribute: 'data-compact',
          description: 'The log is behind a disclosure (`aria-expanded` on its button).',
        },
        {
          name: 'Selected action',
          attribute: 'data-selected / aria-pressed (on the action)',
          description: 'The action whose point is highlighted on the frame.',
        },
        {
          name: 'Confirming stop',
          description: 'The controls are replaced by the question with Stop now and Keep going.',
        },
        {
          name: 'Live',
          attribute: 'data-live',
          description:
            'The live stream is showing; the caption reads "Live" in place of a capture time.',
        },
        {
          name: 'Accepting input',
          attribute: 'data-interactive (on the frame box)',
          description:
            'The person is in control and `onPointer` or `onKey` is given: the screen is focusable and forwards input.',
        },
        {
          name: 'No frame',
          attribute: 'data-empty (on the frame box)',
          description: 'No screenshot yet, or its URL was not allowed; the box keeps its size.',
        },
      ],
    },
    interaction: {
      items: [
        'Pause calls `onPauseChange(true)`, Resume `onPauseChange(false)`; Take over calls `onTakeOver`; Hand back calls `onHandBack` and `onRelease`. The product changes `status` when it has acted.',
        'After Take over, focus moves to Hand back; after Hand back it returns to Take over. A status change the person did not ask for never moves focus.',
        'While taken over with `onPointer` or `onKey`, clicking the screen focuses it; pointer moves, presses, releases and the wheel are forwarded in frame pixels, and keys are forwarded while it has focus.',
        'The controls stick to the bottom edge of a scrolling container, so Stop is always in view while the session can still act.',
        'Stop opens an inline confirmation; only Stop now calls `onStop`. Keep going or Escape backs out and returns focus to Stop.',
        'Selecting an action calls `onSelectAction(id)` and marks its point on the frame. Pass `selectedActionId` to control it.',
        'The log follows the newest action unless the person has scrolled up; scrolling back to the end resumes following.',
        'While `waiting-for-you` the reason is shown and Take over becomes the primary button.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through Take over or Hand back, Pause or Resume, Stop, the log disclosure (compact) and each action.',
        },
        { keys: 'Enter / Space', action: 'Activates the focused button or selects the action.' },
        {
          keys: 'Escape',
          action: 'In the stop confirmation: backs out and returns focus to Stop.',
        },
        {
          keys: 'Any key (screen focused, taken over)',
          action:
            'Sent to `onKey` and not acted on by the page, Tab included, so the remote screen can be driven fully from the keyboard.',
        },
        {
          keys: 'Shift+Escape (screen focused)',
          action: 'Never forwarded: leaves the screen and moves focus to Hand back (or Stop).',
        },
      ],
    },
    accessibility: {
      role: 'group (a named `<section>`)',
      name: '`messages.label`, "Assistant browser view" by default; the log is named "Actions"',
      items: [
        'The screenshot always has a text alternative: the frame `alt` (or a default) plus the latest action label. With no image the same text is kept for assistive technology.',
        'The pointer and action markers are `aria-hidden`; every action they mark is in the log as text with its kind, label and time.',
        'The log is set to `aria-live="off"`; new actions are announced through a separate polite region at most once per `announceInterval`, several at once as "3 new actions. Latest: ...". The first list is not announced.',
        'Each status change is announced once; `waiting-for-you` and `failed` go through `role="alert"` because the person has to act.',
        'Control changes have their own announcements: "You are in control of this browser. The assistant has stopped acting." on take-over and "The assistant is in control of this browser again." on release.',
        'While it accepts input the screen is `role="application"` with a name saying keys and pointer are sent to it, described by a visible hint that names Shift+Escape as the way out, so keyboard focus is never trapped (WCAG 2.1.2).',
        'The live picture is a muted `<video>` with the same text alternative as the screenshot (`aria-label`); it has no controls and takes no focus itself.',
        'Actions are toggle buttons with `aria-pressed`; hidden detail reads "Hidden", so nothing private is exposed to a screen reader either.',
        'Under reduced motion the last-action marker does not pulse and the pointer does not glide.',
      ],
    },
    responsive: {
      items: [
        'Fills its container. From 48rem of its own width the log sits beside the screenshot and takes its height; below that it goes underneath with its own scroll.',
        'The screenshot scales to the available width and keeps its aspect ratio.',
        'On narrow screens or in a narrow AI panel the controls share the width; on coarse pointers controls and actions grow to 44px.',
        'RTL: logical properties throughout; marker positions stay physical because frame pixels count from the left edge.',
      ],
    },
    theme: {
      items: [
        'The banner uses `--mn-color-ai-solid` / `--mn-color-ai-on-solid`; the waiting notice uses the warning tokens and the stop confirmation the danger tokens.',
        'Markers have a surface-coloured halo so they stay visible on any screenshot.',
        'Forced colours: borders use CanvasText, the banner a Highlight border, markers and the selected action Highlight.',
      ],
    },
    examples: {
      items: [
        { title: 'Live session', stack: true, render: () => <LiveDemo /> },
        { title: 'Take over and act on the screen', stack: true, render: () => <TakeOverDemo /> },
        {
          title: 'Waiting for the person',
          stack: true,
          render: () => (
            <StaticDemo
              status="waiting-for-you"
              waitingReason="Payment details are needed. The assistant does not enter them itself."
            />
          ),
        },
        {
          title: 'Taken over',
          stack: true,
          render: () => <StaticDemo status="taken-over" />,
        },
        {
          title: 'Compact',
          stack: true,
          render: () => <StaticDemo status="working" compact />,
        },
        {
          title: 'Before the first screenshot',
          stack: true,
          render: () => <AiComputerView status="starting" onStop={() => {}} />,
        },
      ],
    },
    doDont: {
      do: [
        'Mark screenshots that can show private data with `frame.sensitive`.',
        'Mark password, payment and personal fields as `sensitive` at the source, and keep typed text out of `label`.',
        'Use `waiting-for-you` with a plain reason for steps the agent must not do itself, such as signing in or paying.',
        'Always pass `onStop` while a session can still act.',
        'Set `streamSensitive` (or pause the stream) whenever the screen may show passwords, payment details or personal data.',
        'Tell people before a session starts that the screen is streamed to them and that their input is sent to the remote machine while they are in control.',
      ],
      dont: [
        'Do not turn on `showTypedText` unless the person asked to see what was typed.',
        'Do not change `status` optimistically for control you have not actually taken or returned.',
        'Do not send screenshots as `data:` URLs; serve them or use `blob:` URLs.',
        'Do not act on `onPointer` or `onKey` input server-side unless the session really is taken over: check it there too.',
        'Do not record forwarded keys in logs or analytics.',
      ],
    },
    related: {
      items: [
        { id: 'ai-agent-run-timeline', reason: 'The trace of a run without a screen.' },
        { id: 'ai-task-progress', reason: 'A long task with a bar and steps.' },
        { id: 'ai-action-log', reason: 'An audit list of agent changes with undo.' },
        { id: 'ai-stop-button', reason: 'Stopping a streamed reply.' },
        { id: 'ai-permission-scope', reason: 'What the agent is allowed to do before it starts.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button', 'spinner'] },
    testing: {
      unit: [
        'The position helper converts frame pixels to percentages, clamps outside points and rejects unusable input; the detail helper applies the sensitive and typed-text rules.',
        'Renders a named region with status text, the control banner, the window bar (credentials stripped from the address), the capture time and a `log` of actions with kind, label and time; ref and props forwarding.',
        'Reserves the frame box from width and height, gives the screenshot a text alternative, and never renders an unsafe URL.',
        'The overlay is `aria-hidden` and places the last-action, selected and pointer markers by percentage.',
        'Sensitive detail is always "Hidden"; typed text only shows with `showTypedText`.',
        'Pause, Resume, Take over and Hand back call back from mouse and keyboard; Stop needs the inline confirmation, Escape and Keep going back out with focus returned; controls go away once finished.',
        'The waiting notice shows the reason and makes Take over primary.',
        'Action selection, uncontrolled and controlled; compact disclosure, uncontrolled and controlled.',
        'The log follows the newest action unless scrolled up; new actions are announced throttled and status changes once, urgent ones as an alert.',
        'Translated strings, locale and time zone formatting.',
        'A sensitive action shows and announces “Private action” instead of its label, and the screenshot after it (or a frame marked `sensitive`) is hidden.',
        'Long sessions render the newest `visibleActions` with “Show earlier”; remembered ids are bounded.',
        'A stream with a live video track renders in a muted `<video>` in place of the screenshot, falls back to the frame when the track ends, is detached (not stopped) on unmount, and is hidden by `streamSensitive`, a sensitive frame or a sensitive latest action.',
        'The page-to-frame conversion clamps to the box and rejects unusable input.',
        'Pointer and key input is forwarded in frame pixels only while taken over and the picture is shown; forwarded keys are not acted on by the page; Shift+Escape is never forwarded and moves focus to Hand back.',
        'Focus follows Take over and Hand back, not outside status changes; control changes are announced; Hand back shows from `onRelease` alone and calls both callbacks.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
