'use client';

import {
  createContext,
  isValidElement,
  useCallback,
  useContext,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type ReactNode,
} from 'react';
import * as ToastPrimitive from '@radix-ui/react-toast';
import { cx } from '../../../internal/cx.js';
import { CloseGlyph, InfoGlyph, SuccessGlyph, WarningGlyph } from '../../../internal/icons.js';
import { useDirection } from '../../../internal/direction.js';

export type ToastTone = 'neutral' | 'info' | 'success' | 'warning' | 'danger';

export interface ToastOptions {
  /** Short headline, e.g. "Ticket assigned". */
  title: ReactNode;
  /** One line of detail. Keep it under ~80 characters. */
  description?: ReactNode;
  tone?: ToastTone;
  /** One optional action, e.g. Undo. Rendered as a secondary button. */
  action?: {
    label: string;
    onClick: () => void;
    /**
     * What a screen reader hears instead of the button, describing another
     * way to do the same thing ("Undo from the Archive page"). Defaults to
     * `label`.
     */
    altText?: string;
  };
  /**
   * Milliseconds before auto-dismiss. Danger toasts and toasts with an action
   * default to 10s, others to 5s. `Infinity` keeps the toast until dismissed.
   * The provider's `durationScale` multiplies it.
   */
  duration?: number;
  /**
   * How a screen reader announces the toast. Default: `assertive` for
   * `danger`, `polite` for every other tone. Use `assertive` only for errors
   * the user must hear now.
   */
  politeness?: 'polite' | 'assertive';
  /** Called once when the toast closes, however it closed (timer, Dismiss, Escape, swipe, action, overflow). */
  onDismiss?: () => void;
}

/** `toast.success(title, options)` and friends: the tone is set for you. */
export type ToastShorthand = (
  title: ReactNode,
  options?: Omit<ToastOptions, 'title' | 'tone'>,
) => ToastHandle;

/**
 * `toast(options)` or `toast(title, options)`, plus `toast.success`,
 * `toast.error`, `toast.warning`, `toast.info`.
 */
export interface ToastFunction {
  (options: ToastOptions): ToastHandle;
  (title: ReactNode, options?: Omit<ToastOptions, 'title'>): ToastHandle;
  success: ToastShorthand;
  /** Danger tone. Pair with `getErrorMessage(error)` for errors from an API. */
  error: ToastShorthand;
  warning: ToastShorthand;
  info: ToastShorthand;
}

export interface ToastHandle {
  id: string;
  dismiss: () => void;
}

interface ToastRecord extends ToastOptions {
  id: string;
  open: boolean;
}

export interface ToastContextValue {
  toast: ToastFunction;
  /** Closes one toast, or every open toast when called without an id. */
  dismiss: (id?: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

/** Builds the callable `toast` with its tone shorthands around one `show`. */
function makeToastFunction(show: (options: ToastOptions) => ToastHandle): ToastFunction {
  const fn = ((first: ToastOptions | ReactNode, rest?: Omit<ToastOptions, 'title'>) =>
    show(isOptions(first) ? first : { ...rest, title: first })) as ToastFunction;
  const tone =
    (t: ToastTone): ToastShorthand =>
    (title, options) =>
      show({ ...options, title, tone: t });
  fn.success = tone('success');
  fn.error = tone('danger');
  fn.warning = tone('warning');
  fn.info = tone('info');
  return fn;
}

function isOptions(value: unknown): value is ToastOptions {
  return (
    typeof value === 'object' &&
    value !== null &&
    !Array.isArray(value) &&
    !isValidElement(value) &&
    'title' in value
  );
}

/** Longer than the exit animation (--mn-duration-base). */
const EXIT_MS = 600;
const SHORT_MS = 5000;
const LONG_MS = 10000;
let counter = 0;
const nextId = () => `mn-toast-${++counter}`;

/**
 * Mounted providers, newest last. The global `toast` sends to the newest one.
 * Calls made before any provider has mounted wait in `queued` (newest kept).
 */
interface ToastBridge {
  show: (options: ToastOptions, id: string) => void;
  dismiss: (id?: string) => void;
}
const bridges: ToastBridge[] = [];
const queued: { options: ToastOptions; id: string }[] = [];
const QUEUE_MAX = 10;

/**
 * How far the toast region sits in from each edge of the window, on top of
 * its own padding and the safe-area inset. A number is pixels; a string is
 * any CSS length (`'4rem'`, `'calc(var(--dock-height) + 8px)'`). Each side
 * sets the matching public variable on the viewport
 * (`--mn-toast-offset-block-start`, `--mn-toast-offset-block-end`,
 * `--mn-toast-offset-inline-end`), so a host stylesheet can set those
 * instead when the offset depends on app state.
 */
export interface ToastOffset {
  /** From the top edge; used by `top-right`. */
  blockStart?: number | string;
  /** From the bottom edge; used by `bottom-right` and `bottom-center`. */
  blockEnd?: number | string;
  /** From the end edge (right, or left in right-to-left pages); used by `bottom-right` and `top-right`. */
  inlineEnd?: number | string;
}

const OFFSET_VARS = {
  blockStart: '--mn-toast-offset-block-start',
  blockEnd: '--mn-toast-offset-block-end',
  inlineEnd: '--mn-toast-offset-inline-end',
} as const;

function offsetStyle(offset: ToastOffset | undefined): CSSProperties | undefined {
  if (!offset) return undefined;
  const style: Record<string, string> = {};
  for (const side of Object.keys(OFFSET_VARS) as (keyof ToastOffset)[]) {
    const value = offset[side];
    if (value === undefined || value === '') continue;
    style[OFFSET_VARS[side]] = typeof value === 'number' ? `${value}px` : value;
  }
  return Object.keys(style).length ? (style as CSSProperties) : undefined;
}

export interface ToastProviderProps {
  /** Accessible name of the toast region, also read before each announcement. */
  label?: string;
  /**
   * Where toasts appear. `-right` positions sit at the inline end, so they
   * move to the left edge in right-to-left pages.
   */
  position?: 'bottom-right' | 'bottom-center' | 'top-right';
  /**
   * Moves the region in from the window edges, for example clear of a docked
   * panel. Sets the public `--mn-toast-offset-*` variables on the viewport;
   * a value given here wins over the same variable set in a stylesheet.
   */
  offset?: ToastOffset;
  /**
   * Keys that move focus to the toast region, as `KeyboardEvent.code` or
   * modifier names (`['altKey', 'KeyT']`). Default `['F8']`. The hint is
   * added to the region's name ("Notifications (F8)").
   */
  hotkey?: string[];
  /**
   * Multiplies every auto-dismiss duration (WCAG 2.2.1 Timing Adjustable).
   * Wire it to a user preference: `2` doubles the time, `Infinity` turns
   * auto-dismiss off. Default 1.
   */
  durationScale?: number;
  /** How many toasts are shown at once; older ones close first. Default 3. */
  limit?: number;
  /** Accessible name of each toast's close button. Default "Dismiss". */
  dismissLabel?: string;
  children: ReactNode;
}

/**
 * Mount once near the root. Provides `useToast()` and renders the viewport.
 *
 * Toasts never take focus. They pause while hovered or focused, can be reached
 * with F8 (the `hotkey`), and close with Escape. Each one is announced once
 * through a live region: assertively for `danger`, politely otherwise.
 */
export function ToastProvider({
  label = 'Notifications',
  position = 'bottom-right',
  hotkey = DEFAULT_HOTKEY,
  durationScale = 1,
  limit = 3,
  dismissLabel = 'Dismiss',
  offset,
  children,
}: ToastProviderProps) {
  const readDir = useDirection();
  const [toasts, setToasts] = useState<ToastRecord[]>([]);
  // Right positions sit at the inline end, so in a right-to-left page they are
  // on the left and are swiped away to the left.
  const viewportRef = useRef<HTMLOListElement>(null);
  const [rtl, setRtl] = useState(false);
  useEffect(() => {
    setRtl(readDir(viewportRef.current) === 'rtl');
  }, [readDir]);
  const timers = useRef(new Set<ReturnType<typeof setTimeout>>());
  // Ids whose onDismiss has run. A closed record is only removed after that,
  // so a sweep that runs before React renders the close cannot skip onDismiss.
  const notified = useRef(new Set<string>());

  useEffect(() => {
    const pending = timers.current;
    return () => {
      for (const t of pending) clearTimeout(t);
      pending.clear();
    };
  }, []);

  /** Drops closed records once the exit animation has had time to run. */
  const sweep = useCallback(() => {
    const t = setTimeout(() => {
      timers.current.delete(t);
      setToasts((list) => {
        const next = list.filter((r) => r.open || !notified.current.has(r.id));
        for (const r of list) if (!next.includes(r)) notified.current.delete(r.id);
        return next.length === list.length ? list : next;
      });
    }, EXIT_MS);
    timers.current.add(t);
  }, []);

  const dismiss = useCallback(
    (id?: string) => {
      setToasts((list) =>
        list.map((t) => (t.open && (id === undefined || t.id === id) ? { ...t, open: false } : t)),
      );
      sweep();
    },
    [sweep],
  );

  const show = useCallback(
    (options: ToastOptions, id: string = nextId()): ToastHandle => {
      setToasts((list) => {
        const next = [...list, { ...options, id, open: true }];
        const openOnes = next.filter((t) => t.open);
        const overflow = openOnes.length - Math.max(1, limit);
        if (overflow > 0) {
          const set = new Set(openOnes.slice(0, overflow).map((t) => t.id));
          return next.map((t) => (set.has(t.id) ? { ...t, open: false } : t));
        }
        return next;
      });
      sweep();
      return { id, dismiss: () => dismiss(id) };
    },
    [dismiss, sweep, limit],
  );

  const toast = useMemo(() => makeToastFunction((options) => show(options)), [show]);

  // Register with the global `toast` and deliver anything sent before mount.
  const showRef = useRef(show);
  const dismissRef = useRef(dismiss);
  useEffect(() => {
    showRef.current = show;
    dismissRef.current = dismiss;
  });
  useEffect(() => {
    const bridge: ToastBridge = {
      show: (options, id) => showRef.current(options, id),
      dismiss: (id) => dismissRef.current(id),
    };
    bridges.push(bridge);
    for (const q of queued.splice(0)) bridge.show(q.options, q.id);
    return () => {
      const i = bridges.indexOf(bridge);
      if (i !== -1) bridges.splice(i, 1);
    };
  }, []);

  const value = useMemo(() => ({ toast, dismiss }), [toast, dismiss]);

  return (
    <ToastContext.Provider value={value}>
      <ToastPrimitive.Provider
        swipeDirection={position.endsWith('right') ? (rtl ? 'left' : 'right') : 'down'}
        label={label}
      >
        {children}
        {toasts.map((t) => (
          <ToastItem
            key={t.id}
            record={t}
            durationScale={durationScale}
            dismissLabel={dismissLabel}
            onOpenChange={(open) => (open ? undefined : dismiss(t.id))}
            onNotified={() => {
              notified.current.add(t.id);
              sweep();
            }}
          />
        ))}
        <ToastPrimitive.Viewport
          ref={viewportRef}
          className="mn-toast-viewport"
          data-position={position}
          style={offsetStyle(offset)}
          hotkey={hotkey}
          label={hotkey.length ? `${label} ({hotkey})` : label}
        />
      </ToastPrimitive.Provider>
    </ToastContext.Provider>
  );
}

const DEFAULT_HOTKEY = ['F8'];

const toneIcon = {
  neutral: null,
  info: <InfoGlyph />,
  success: <SuccessGlyph />,
  warning: <WarningGlyph />,
  danger: <WarningGlyph />,
} as const;

/** The duration Radix should use: scaled, and Infinity when auto-dismiss is off. */
function toastDuration(record: ToastOptions, scale: number): number {
  const base = record.duration ?? (record.tone === 'danger' || record.action ? LONG_MS : SHORT_MS);
  const scaled = base * (scale > 0 ? scale : 1);
  return Number.isFinite(scaled) ? Math.round(scaled) : Infinity;
}

function ToastItem({
  record,
  durationScale,
  dismissLabel,
  onOpenChange,
  onNotified,
}: {
  record: ToastRecord;
  durationScale: number;
  dismissLabel: string;
  onOpenChange: (open: boolean) => void;
  onNotified: () => void;
}) {
  const titleId = useId();
  const tone = record.tone ?? 'neutral';
  const politeness = record.politeness ?? (tone === 'danger' ? 'assertive' : 'polite');
  const icon = toneIcon[tone];

  // onDismiss fires once, the first time the record closes.
  const notified = useRef(false);
  const onDismiss = record.onDismiss;
  const notifiedRef = useRef(onNotified);
  notifiedRef.current = onNotified;
  useEffect(() => {
    if (!record.open && !notified.current) {
      notified.current = true;
      onDismiss?.();
      notifiedRef.current();
    }
  }, [record.open, onDismiss]);

  return (
    // No role on the <li>: it must stay a list item of the viewport's <ol>.
    // Radix announces the text through its own live region, whose politeness
    // follows `type` (foreground = assertive, background = polite).
    <ToastPrimitive.Root
      open={record.open}
      onOpenChange={onOpenChange}
      duration={toastDuration(record, durationScale)}
      type={politeness === 'assertive' ? 'foreground' : 'background'}
      className="mn-toast"
      data-tone={tone}
      data-politeness={politeness}
      aria-labelledby={titleId}
    >
      {icon ? (
        <span className="mn-toast__icon" aria-hidden>
          {icon}
        </span>
      ) : null}
      <div className="mn-toast__body">
        <ToastPrimitive.Title id={titleId} className="mn-toast__title">
          {record.title}
        </ToastPrimitive.Title>
        {record.description ? (
          <ToastPrimitive.Description className="mn-toast__description">
            {record.description}
          </ToastPrimitive.Description>
        ) : null}
      </div>
      {record.action ? (
        <ToastPrimitive.Action asChild altText={record.action.altText || record.action.label}>
          <button
            type="button"
            className={cx('mn-button', 'mn-toast__action')}
            data-variant="secondary"
            data-size="xs"
            onClick={record.action.onClick}
          >
            {record.action.label}
          </button>
        </ToastPrimitive.Action>
      ) : null}
      <ToastPrimitive.Close
        className="mn-toast__close"
        aria-label={dismissLabel}
        aria-describedby={titleId}
      >
        <CloseGlyph />
      </ToastPrimitive.Close>
    </ToastPrimitive.Root>
  );
}

/**
 * Returns `toast` (callable, with `.success`, `.error`, `.warning`, `.info`)
 * and `dismiss(id?)`. Must be used inside ToastProvider.
 */
export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used inside <ToastProvider>');
  return ctx;
}

/** The global `toast`: the same calls as `useToast().toast`, plus `toast.dismiss(id?)`. */
export interface GlobalToastFunction extends ToastFunction {
  /** Closes one toast, or every open toast when called without an id. */
  dismiss: (id?: string) => void;
}

function globalShow(options: ToastOptions): ToastHandle {
  const id = nextId();
  const target = bridges[bridges.length - 1];
  if (target) target.show(options, id);
  else if (typeof window !== 'undefined') {
    queued.push({ options, id });
    if (queued.length > QUEUE_MAX) queued.shift();
  }
  return { id, dismiss: () => toast.dismiss(id) };
}

/**
 * Shows a toast from anywhere: event handlers, API clients, stores, plain
 * modules. It goes to the most recently mounted `ToastProvider`; calls made
 * before one mounts are shown once it does. On the server it does nothing.
 *
 * ```ts
 * import { toast } from '@rookcom/monarch-react';
 * toast.success('Saved');
 * toast.error('Could not save', { description: getErrorMessage(error) });
 * ```
 */
export const toast: GlobalToastFunction = Object.assign(makeToastFunction(globalShow), {
  dismiss(id?: string) {
    if (id === undefined) queued.length = 0;
    else {
      const i = queued.findIndex((q) => q.id === id);
      if (i !== -1) queued.splice(i, 1);
    }
    bridges[bridges.length - 1]?.dismiss(id);
  },
});

/** Messages that describe the transport, not the problem; replaced by the fallback. */
const GENERIC = [
  /^request failed with status code \d{3}$/i,
  /^network ?error$/i,
  /^failed to fetch$/i,
  /^load failed$/i,
  /^networkerror when attempting to fetch resource\.?$/i,
  /^internal server error$/i,
  /^\[object object\]$/i,
  // An HTML error page from a proxy or gateway.
  /^<(!doctype|html|head|body)\b/i,
];

export interface GetErrorMessageOptions {
  /** Used when nothing readable can be found. Default "Something went wrong. Try again." */
  fallback?: string;
  /** Used for network failures (offline, DNS, CORS). Default: `fallback`. */
  network?: string;
}

function text(value: unknown): string | undefined {
  if (typeof value === 'string') {
    const t = value.trim();
    return t && !GENERIC.some((re) => re.test(t)) ? t : undefined;
  }
  if (Array.isArray(value)) {
    const parts = value
      .map((v) =>
        text(
          typeof v === 'object' && v
            ? ((v as Record<string, unknown>).message ?? (v as Record<string, unknown>).detail)
            : v,
        ),
      )
      .filter(Boolean);
    return parts.length ? parts.join(' ') : undefined;
  }
  return undefined;
}

function fromBody(body: unknown): string | undefined {
  if (!body || typeof body !== 'object') return text(body);
  const b = body as Record<string, unknown>;
  const details = b.details as Record<string, unknown> | undefined;
  const fields = (details?.fields ?? b.fields ?? b.fieldErrors) as
    Record<string, unknown> | undefined;
  if (fields && typeof fields === 'object') {
    const flat = Object.values(fields).flatMap((v) => (Array.isArray(v) ? v : [v]));
    const joined = text(flat);
    if (joined) return joined;
  }
  return text(b.message) ?? text(b.errors) ?? text(b.detail) ?? text(b.error) ?? text(b.title);
}

/**
 * Turns anything thrown or rejected into one sentence a person can read, for
 * `toast.error()` or an inline error. Pure; it knows no product's API.
 *
 * Looks, in order, at: a string; field errors (`details.fields`, `fields`,
 * `fieldErrors`, flattened); `message` (string or list); `errors[]`
 * (`message`/`detail`); `detail`; `error`; `title`, on the response body
 * (`error.response.data`, `error.data`, `error.body`) and then the error
 * itself. Transport messages ("Request failed with status code 500",
 * "Failed to fetch") are never shown; the fallback is.
 */
export function getErrorMessage(error: unknown, options: GetErrorMessageOptions = {}): string {
  const fallback = options.fallback ?? 'Something went wrong. Try again.';
  if (error == null) return fallback;
  if (typeof error === 'string') return text(error) ?? fallback;
  if (typeof error !== 'object') return fallback;
  const e = error as Record<string, unknown>;
  const response = e.response as Record<string, unknown> | undefined;
  const found = fromBody(response?.data) ?? fromBody(e.data) ?? fromBody(e.body) ?? fromBody(e);
  if (found) return found;
  const raw = typeof e.message === 'string' ? e.message.trim() : '';
  const isNetwork =
    e.name === 'TypeError' ||
    e.code === 'ERR_NETWORK' ||
    /^(network ?error|failed to fetch|load failed|networkerror)/i.test(raw);
  if (isNetwork && options.network) return options.network;
  return fallback;
}
