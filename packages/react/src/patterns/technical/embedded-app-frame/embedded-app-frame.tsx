'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
  type CSSProperties,
  type HTMLAttributes,
  type IframeHTMLAttributes,
  type ReactNode,
  type Ref,
} from 'react';
import { Button } from '../../../components/actions/button/index.js';
import { Spinner } from '../../../components/feedback/spinner/index.js';
import { VisuallyHidden } from '../../../components/layout/visually-hidden/index.js';
import { cx } from '../../../internal/cx.js';
import {
  allowValue,
  documentBase,
  exactOrigins,
  originOf,
  sandboxValue,
  type EmbedSandboxToken,
} from '../../../internal/shell/embed.js';
import { focusBeside } from '../../../internal/shell/focus.js';
import { ExternalGlyph } from '../../../internal/shell/icons.js';
import { useVisibleTimeout } from '../../../internal/shell/use-visible-timeout.js';
import {
  APP_EMBED_PROTOCOL,
  decodeAppEmbedChildMessage,
  encodeAppEmbedMessage,
  type AppEmbedHostMessage,
  type EmbedFocusDirection,
} from './embedded-app-frame.protocol.js';

export type { EmbedSandboxToken } from '../../../internal/shell/embed.js';

/** Where the frame is: waiting for `ready`, showing the page, or given up. */
export type EmbeddedAppFrameStatus = 'loading' | 'ready' | 'error';

export interface EmbeddedAppFrameMessages {
  /** Shown while waiting for the page. Default "Loading…". */
  loading: string;
  /** Error heading. Default "This page didn’t open". */
  errorTitle: string;
  /** Error text. */
  errorDescription: string;
  /** Default "Try again". */
  retry: string;
  /** The open-in-app link. Default "Open in a new tab". */
  open: string;
  /** Appended to the open link for screen readers. Default "opens in a new tab". */
  newTab: string;
}

const DEFAULT_MESSAGES: EmbeddedAppFrameMessages = {
  loading: 'Loading…',
  errorTitle: 'This page didn’t open',
  errorDescription:
    'The app didn’t respond in time. It may be briefly unavailable. Try again, or open the page directly.',
  retry: 'Try again',
  open: 'Open in a new tab',
  newTab: 'opens in a new tab',
};

/**
 * The default sandbox: the embedded Rook page may run scripts, keep its own
 * same-origin session (cookies) and submit forms. It may not open popups,
 * download files, show modal dialogs or navigate the host page.
 */
export const DEFAULT_EMBED_SANDBOX: readonly EmbedSandboxToken[] = [
  'allow-scripts',
  'allow-same-origin',
  'allow-forms',
];

export interface EmbeddedAppFrameProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'title' | 'children' | 'onError'
> {
  /** The page to embed (absolute URL, or relative to this page). */
  src: string;
  /** The iframe's accessible name, e.g. "Rook Tickets: Email settings". Required. */
  title: string;
  /**
   * Exact origins whose messages are accepted. Default: the origin of `src`
   * only. Entries that are not a bare origin (`*`, paths) are ignored.
   */
  allowedOrigins?: readonly string[] | undefined;
  /** Protocol name in every message. Default `rook-app-settings` (App Settings contract v1). */
  protocol?: string | undefined;
  /** Sandbox tokens. Default `DEFAULT_EMBED_SANDBOX` (scripts, same-origin, forms). */
  sandbox?: readonly EmbedSandboxToken[] | undefined;
  /** Permissions policy features, e.g. `['clipboard-write']`. Default none. */
  allow?: readonly string[] | undefined;
  /** Default `strict-origin-when-cross-origin`. */
  referrerPolicy?: IframeHTMLAttributes<HTMLIFrameElement>['referrerPolicy'];
  /** Follow the page's `resize` messages. Default true. */
  autoHeight?: boolean | undefined;
  /**
   * Height before the first `resize` message, or the fixed height without
   * `autoHeight` (a number of pixels or any CSS length). Default 480.
   */
  height?: number | string | undefined;
  /** Smallest auto height in pixels. Default 120. */
  minHeight?: number | undefined;
  /** Largest auto height in pixels; the page scrolls inside the frame beyond it. */
  maxHeight?: number | undefined;
  /** Give up after this much visible time without `ready`. Default 12000 ms; 0 waits forever. */
  readyTimeout?: number | undefined;
  /** Link offered with the error (the page in its own app). Opens in a new tab. */
  openHref?: string | undefined;
  /** Move focus into the page once it is ready. Default false. */
  focusOnReady?: boolean | undefined;
  onReady?: (() => void) | undefined;
  onStatusChange?: ((status: EmbeddedAppFrameStatus) => void) | undefined;
  /**
   * The page handed focus back (`focus-exit`). Default: focus moves to the
   * control after (forward) or before (backward) the frame.
   */
  onFocusExit?: ((direction: EmbedFocusDirection) => void) | undefined;
  /** The iframe element. */
  iframeRef?: Ref<HTMLIFrameElement> | undefined;
  /** Replaces the loading content. */
  loadingContent?: ReactNode;
  /** Interface text, for translation. */
  messages?: Partial<EmbeddedAppFrameMessages> | undefined;
}

function cssLength(v: number | string): string {
  return typeof v === 'number' ? `${v}px` : v;
}

/**
 * Embeds a page of another Rook app (App Settings embed contract v1): an
 * iframe with a minimal sandbox, a ready handshake, auto-height and focus
 * handoff over postMessage, and loading and error states with Try again. Only
 * messages from the frame's own window at an allowed origin are read, and
 * nothing is ever posted to `*`.
 */
export const EmbeddedAppFrame = forwardRef<HTMLDivElement, EmbeddedAppFrameProps>(
  function EmbeddedAppFrame(
    {
      src,
      title,
      allowedOrigins,
      protocol = APP_EMBED_PROTOCOL,
      sandbox = DEFAULT_EMBED_SANDBOX,
      allow = [],
      referrerPolicy = 'strict-origin-when-cross-origin',
      autoHeight = true,
      height = 480,
      minHeight = 120,
      maxHeight,
      readyTimeout = 12_000,
      openHref,
      focusOnReady = false,
      onReady,
      onStatusChange,
      onFocusExit,
      iframeRef,
      loadingContent,
      messages: messagesProp,
      className,
      style,
      ...rest
    },
    ref,
  ) {
    const messages = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
    const frame = useRef<HTMLIFrameElement>(null);
    const root = useRef<HTMLDivElement>(null);
    const overlay = useRef<HTMLDivElement>(null);
    useImperativeHandle(ref, () => root.current as HTMLDivElement);
    useImperativeHandle(iframeRef, () => frame.current as HTMLIFrameElement, []);

    const [status, setStatusState] = useState<EmbeddedAppFrameStatus>('loading');
    const [attempt, setAttempt] = useState(0);
    const [reported, setReported] = useState<number | null>(null);
    const [retried, setRetried] = useState(false);

    // Resolved after mount so server and client render the same markup.
    const [base, setBase] = useState<string | undefined>(undefined);
    useEffect(() => setBase(documentBase()), []);
    const srcOrigin = originOf(src, base);
    const allowed = useMemo(
      () => exactOrigins(allowedOrigins ?? (srcOrigin ? [srcOrigin] : [])),
      // A new array with the same entries must not reset the listener.
      // eslint-disable-next-line react-hooks/exhaustive-deps
      [srcOrigin, (allowedOrigins ?? []).join('\n')],
    );

    const latest = useRef({ onReady, onStatusChange, onFocusExit, focusOnReady });
    useEffect(() => {
      latest.current = { onReady, onStatusChange, onFocusExit, focusOnReady };
    });

    const statusRef = useRef<EmbeddedAppFrameStatus>('loading');
    const setStatus = useCallback((s: EmbeddedAppFrameStatus) => {
      if (statusRef.current === s) return;
      statusRef.current = s;
      setStatusState(s);
      latest.current.onStatusChange?.(s);
    }, []);

    const post = useCallback(
      (message: AppEmbedHostMessage) => {
        const win = frame.current?.contentWindow;
        // Only ever to the page's own origin, and only when that origin is allowed.
        if (!win || !srcOrigin || !allowed.has(srcOrigin)) return;
        try {
          win.postMessage(encodeAppEmbedMessage(message, protocol), srcOrigin);
        } catch {
          /* the frame navigated away: nothing to tell */
        }
      },
      [allowed, srcOrigin, protocol],
    );

    useEffect(() => {
      const onMessage = (e: MessageEvent) => {
        const win = frame.current?.contentWindow;
        if (!win || e.source !== win || !allowed.has(e.origin)) return;
        const m = decodeAppEmbedChildMessage(e.data, protocol);
        if (!m) return;
        if (m.type === 'ready') {
          if (statusRef.current !== 'ready') {
            setStatus('ready');
            latest.current.onReady?.();
            if (latest.current.focusOnReady) {
              frame.current?.focus();
              post({ type: 'focus', direction: 'forward' });
            }
          }
        } else if (m.type === 'resize') {
          setReported(m.height);
        } else if (m.type === 'focus-exit') {
          if (latest.current.onFocusExit) latest.current.onFocusExit(m.direction);
          else if (frame.current && !focusBeside(frame.current, m.direction)) root.current?.focus();
        }
      };
      window.addEventListener('message', onMessage);
      return () => window.removeEventListener('message', onMessage);
    }, [allowed, protocol, post, setStatus]);

    // Each (re)load starts over.
    useEffect(() => {
      setStatus('loading');
    }, [src, attempt, setStatus]);

    useVisibleTimeout(
      status === 'loading',
      readyTimeout,
      () => {
        if (statusRef.current === 'loading') setStatus('error');
      },
      `${src}\u0000${attempt}`,
    );

    // After Try again, keep focus in the frame's area instead of losing it to the page.
    useEffect(() => {
      if (retried && status === 'loading') overlay.current?.focus();
    }, [retried, status, attempt]);

    const retry = () => {
      setReported(null);
      setRetried(true);
      setAttempt((n) => n + 1);
    };

    const fixed = !autoHeight || reported === null;
    const autoPx =
      reported === null
        ? null
        : Math.max(minHeight, maxHeight !== undefined ? Math.min(reported, maxHeight) : reported);
    const frameHeight = fixed ? cssLength(height) : `${autoPx}px`;

    return (
      <div
        ref={root}
        tabIndex={-1}
        {...rest}
        className={cx('mn-embedded-app-frame', className)}
        data-status={status}
        data-auto-height={autoHeight || undefined}
        style={
          { ...style, ['--mn-embedded-app-frame-height' as string]: frameHeight } as CSSProperties
        }
      >
        <iframe
          key={attempt}
          ref={frame}
          className="mn-embedded-app-frame__frame"
          src={src}
          title={title}
          sandbox={sandboxValue(sandbox)}
          allow={allowValue(allow)}
          referrerPolicy={referrerPolicy}
          // A page that announced `ready` before this listener existed says it again.
          onLoad={() => post({ type: 'ping' })}
          aria-hidden={status === 'error' || undefined}
          tabIndex={status === 'error' ? -1 : undefined}
        />
        {status === 'loading' ? (
          <div ref={overlay} className="mn-embedded-app-frame__overlay" role="status" tabIndex={-1}>
            {loadingContent ?? (
              <>
                <Spinner size="md" />
                <span>{messages.loading}</span>
              </>
            )}
          </div>
        ) : null}
        {status === 'error' ? (
          <div className="mn-embedded-app-frame__overlay" data-error="">
            <div className="mn-embedded-app-frame__error" role="alert">
              <p className="mn-embedded-app-frame__error-title">{messages.errorTitle}</p>
              <p className="mn-embedded-app-frame__error-text">{messages.errorDescription}</p>
            </div>
            <div className="mn-embedded-app-frame__actions">
              <Button variant="secondary" size="sm" onClick={retry}>
                {messages.retry}
              </Button>
              {openHref ? (
                <Button asChild variant="ghost" size="sm" trailingIcon={<ExternalGlyph />}>
                  <a href={openHref} target="_blank" rel="noopener noreferrer">
                    {messages.open}
                    <VisuallyHidden> ({messages.newTab})</VisuallyHidden>
                  </a>
                </Button>
              ) : null}
            </div>
          </div>
        ) : null}
      </div>
    );
  },
);
