'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useImperativeHandle,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
  type Ref,
} from 'react';
import { Button, IconButton } from '../../../components/actions/button/index.js';
import { Spinner } from '../../../components/feedback/spinner/index.js';
import { cx } from '../../../internal/cx.js';
import {
  allowValue,
  documentBase,
  exactOrigins,
  originOf,
  sandboxValue,
  type EmbedSandboxToken,
} from '../../../internal/shell/embed.js';
import { ChatGlyph, CloseGlyph } from '../../../internal/shell/panel-icons.js';
import {
  chatPageContext,
  cleanOpenDetail,
  createChatDockBridge,
  type ChatDockBridge,
  type ChatDockLayout,
  type ChatEmbedHostMessage,
  type ChatEmbedMessage,
  type ChatEmbedStatus,
  type ChatHostContext,
  type ChatOpenDetail,
} from './chat-dock.protocol.js';

export interface ChatDockMessages {
  /** The launcher and panel name. Default "Rook Chat". */
  label: string;
  /** Appended to the launcher name when there are unread conversations. */
  unread: (count: number) => string;
  /** Default "Loading chat…". */
  loading: string;
  /** Default "Incoming call". */
  incomingCall: string;
  /** Default "Open". */
  answer: string;
  /** Default "Dismiss". */
  dismiss: string;
}

const DEFAULT_MESSAGES: ChatDockMessages = {
  label: 'Rook Chat',
  unread: (n) => `${n} unread`,
  loading: 'Loading chat…',
  incomingCall: 'Incoming call',
  answer: 'Open',
  dismiss: 'Dismiss',
};

/** Imperative control of the dock, for "Share in Chat" buttons and the like. */
export interface ChatDockController {
  /** Opens the panel, optionally on a conversation, a share or a create request. */
  open: (detail?: ChatOpenDetail) => void;
  close: () => void;
  /** Sends a message to the frame (queued until it is ready). */
  post: (message: ChatEmbedHostMessage) => void;
}

/**
 * Sandbox for the Chat frame: scripts and its own session, forms, links that
 * open in a new tab and file downloads. No top navigation, no modals.
 */
export const DEFAULT_CHAT_SANDBOX: readonly EmbedSandboxToken[] = [
  'allow-scripts',
  'allow-same-origin',
  'allow-forms',
  'allow-popups',
  'allow-popups-to-escape-sandbox',
  'allow-downloads',
];

/** Calls, screen sharing, fullscreen video, picture-in-picture and copy/paste. */
export const DEFAULT_CHAT_ALLOW: readonly string[] = [
  'camera',
  'microphone',
  'display-capture',
  'fullscreen',
  'picture-in-picture',
  'autoplay',
  'clipboard-read',
  'clipboard-write',
];

export interface ChatDockProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'hidden' | 'title'
> {
  /** The Chat embed page (e.g. `https://chat.example.com/embed/dock?ws=…`). */
  src: string;
  /**
   * Exact origins the frame's messages are accepted from. Messages are posted
   * only to the origin of `src`, and only when it is in this list.
   */
  allowedOrigins: readonly string[];
  /** The host app, e.g. "Rook Stride" (sent in `init`, shown in shared links). */
  appName: string;
  /** The Rook ID workspace id (sent in `init`). */
  workspaceId: string;
  /**
   * The host app's own single-flight session refresh. The dock calls it only
   * when the frame asks; it never refreshes the session or reloads the frame
   * on its own.
   */
  refreshSession: () => Promise<unknown>;
  /**
   * Hide the launcher and panel (the frame stays alive and connected), e.g.
   * on pages with their own AI assistant. A call in progress stays on screen.
   */
  hidden?: boolean | undefined;
  /** Unread count known before the frame runs (the product's own poll). The frame's count wins once it reports. */
  unread?: number | undefined;
  onUnreadChange?: ((count: number) => void) | undefined;
  /** Panel open (controlled). */
  open?: boolean | undefined;
  defaultOpen?: boolean | undefined;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** The page the person is on. Default: this URL without the fragment, and the document title. */
  context?: (() => ChatHostContext) | undefined;
  /** Create the frame on hover or focus of the launcher, so opening feels instant. Default true. */
  mountOnIntent?: boolean | undefined;
  /** Launcher icon. Default: a chat glyph. Pass the product's Chat mark. Decorative. */
  icon?: ReactNode;
  /** Sandbox tokens for the frame. Default `DEFAULT_CHAT_SANDBOX`. */
  sandbox?: readonly EmbedSandboxToken[] | undefined;
  /** Permissions policy for the frame. Default `DEFAULT_CHAT_ALLOW`. */
  allow?: readonly string[] | undefined;
  /** How long an unanswered ring stays. Default 30000 ms. */
  ringTimeout?: number | undefined;
  onStatusChange?: ((status: ChatEmbedStatus) => void) | undefined;
  onLayoutChange?: ((layout: ChatDockLayout) => void) | undefined;
  /** The conversation now open in the frame (null: none). */
  onLocationChange?: ((conversationId: string | null) => void) | undefined;
  /** Every valid message from the frame, after the dock handled it. */
  onEmbedMessage?: ((message: ChatEmbedMessage) => void) | undefined;
  controllerRef?: Ref<ChatDockController> | undefined;
  /** Interface text, for translation. */
  messages?: Partial<ChatDockMessages> | undefined;
}

interface Ring {
  callId: string;
  conversationId: string;
}

/**
 * The Rook Chat dock: a launcher with an unread badge and the panel that holds
 * the real Rook Chat app in an iframe (`rook-chat-embed` v1). The frame is
 * created on first open (or on hover) and then kept alive, so calls and
 * drafts survive closing the panel. The dock never fetches: unread counts and
 * session refreshes come from the product and the frame.
 */
export const ChatDock = forwardRef<HTMLDivElement, ChatDockProps>(function ChatDock(
  {
    src,
    allowedOrigins,
    appName,
    workspaceId,
    refreshSession,
    hidden = false,
    unread: unreadProp,
    onUnreadChange,
    open: openProp,
    defaultOpen = false,
    onOpenChange,
    context,
    mountOnIntent = true,
    icon,
    sandbox = DEFAULT_CHAT_SANDBOX,
    allow = DEFAULT_CHAT_ALLOW,
    ringTimeout = 30_000,
    onStatusChange,
    onLayoutChange,
    onLocationChange,
    onEmbedMessage,
    controllerRef,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
  const panelId = `${useId()}-chat-panel`;
  const frameRef = useRef<HTMLIFrameElement>(null);
  const launcherRef = useRef<HTMLButtonElement>(null);

  const [innerOpen, setInnerOpen] = useState(defaultOpen);
  const open = openProp ?? innerOpen;
  const [mounted, setMounted] = useState(open);
  const [ready, setReady] = useState(false);
  const [status, setStatus] = useState<ChatEmbedStatus>('ok');
  const [layout, setLayout] = useState<ChatDockLayout>('panel');
  const [frameUnread, setFrameUnread] = useState<number | null>(null);
  const [ring, setRing] = useState<Ring | null>(null);

  const [base, setBase] = useState<string | undefined>(undefined);
  useEffect(() => setBase(documentBase()), []);
  const target = originOf(src, base);
  const allowedKey = allowedOrigins.join('\n');
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const allowed = useMemo(() => exactOrigins(allowedOrigins), [allowedKey]);

  const latest = useRef({
    appName,
    workspaceId,
    refreshSession,
    context,
    onOpenChange,
    onUnreadChange,
    onStatusChange,
    onLayoutChange,
    onLocationChange,
    onEmbedMessage,
  });
  useEffect(() => {
    latest.current = {
      appName,
      workspaceId,
      refreshSession,
      context,
      onOpenChange,
      onUnreadChange,
      onStatusChange,
      onLayoutChange,
      onLocationChange,
      onEmbedMessage,
    };
  });

  const setOpen = useCallback(
    (next: boolean) => {
      if (openProp === undefined) setInnerOpen(next);
      latest.current.onOpenChange?.(next);
    },
    [openProp],
  );

  const focusLauncher = () => requestAnimationFrame(() => launcherRef.current?.focus());

  // One bridge per frame origin; created lazily, never while rendering.
  const bridgeRef = useRef<{ key: string; bridge: ChatDockBridge } | null>(null);
  const getBridge = useCallback((): ChatDockBridge => {
    const key = `${target ?? ''}\u0000${allowedKey}`;
    if (!bridgeRef.current || bridgeRef.current.key !== key) {
      bridgeRef.current = {
        key,
        bridge: createChatDockBridge({
          frameWindow: () => frameRef.current?.contentWindow ?? null,
          allowedOrigins: allowed,
          targetOrigin: target,
          appName: () => latest.current.appName,
          workspaceId: () => latest.current.workspaceId,
          refresh: () => latest.current.refreshSession(),
          context: () =>
            (latest.current.context ?? (() => chatPageContext(latest.current.appName)))(),
          on: (m) => {
            switch (m.type) {
              case 'rook-chat:ready':
                setReady(true);
                break;
              case 'rook-chat:unread':
                setFrameUnread(m.count);
                latest.current.onUnreadChange?.(m.count);
                break;
              case 'rook-chat:layout':
                setLayout(m.layout);
                latest.current.onLayoutChange?.(m.layout);
                break;
              case 'rook-chat:ring':
                setRing({ callId: m.callId, conversationId: m.conversationId });
                break;
              case 'rook-chat:close':
                closeRef.current();
                break;
              case 'rook-chat:status':
                setStatus(m.status);
                latest.current.onStatusChange?.(m.status);
                break;
              case 'rook-chat:location':
                latest.current.onLocationChange?.(m.conversationId);
                break;
              default:
                break;
            }
            latest.current.onEmbedMessage?.(m);
          },
        }),
      };
    }
    return bridgeRef.current.bridge;
  }, [target, allowedKey, allowed]);

  useEffect(() => {
    const onMessage = (e: MessageEvent) => void getBridge().handle(e);
    window.addEventListener('message', onMessage);
    return () => window.removeEventListener('message', onMessage);
  }, [getBridge]);

  // A new frame (other workspace or embed URL) must say "ready" again.
  const frameKey = `${src}\u0000${workspaceId}`;
  useEffect(() => {
    getBridge().reset();
    setReady(false);
    setFrameUnread(null);
    setLayout('panel');
  }, [frameKey, getBridge]);

  const inCall = layout === 'call' || layout === 'fullscreen';
  const shown = status !== 'unavailable' && (!hidden || inCall);
  const panelVisible = shown && open && mounted;

  const close = useCallback(() => {
    setOpen(false);
    focusLauncher();
  }, [setOpen]);
  const closeRef = useRef(close);
  closeRef.current = close;

  const openDock = useCallback(
    (detail?: ChatOpenDetail) => {
      setMounted(true);
      setOpen(true);
      setRing(null);
      const clean = cleanOpenDetail(detail);
      if (clean.conversationId || clean.share || clean.create)
        getBridge().post({ type: 'rook-chat:open', ...clean });
      requestAnimationFrame(() => frameRef.current?.focus());
    },
    [getBridge, setOpen],
  );

  useImperativeHandle(
    controllerRef,
    () => ({ open: openDock, close, post: (m) => getBridge().post(m) }),
    [openDock, close, getBridge],
  );

  // Open from outside (controlled `open`) mounts the frame too.
  useEffect(() => {
    if (open) setMounted(true);
  }, [open]);

  // Keep the frame told whether it is on screen.
  useEffect(() => {
    if (ready) getBridge().post({ type: 'rook-chat:visibility', open: panelVisible });
  }, [ready, panelVisible, getBridge]);

  // A ring nobody answers goes away on its own.
  useEffect(() => {
    if (!ring || !(ringTimeout > 0)) return;
    const t = setTimeout(() => setRing(null), ringTimeout);
    return () => clearTimeout(t);
  }, [ring, ringTimeout]);

  const unread = frameUnread ?? Math.max(0, Math.floor(unreadProp ?? 0));
  const launcherName =
    unread > 0 ? `${messages.label}, ${messages.unread(unread)}` : messages.label;

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-chat-dock', className)}
      data-layout={layout}
      data-open={panelVisible || undefined}
      data-hidden={!shown || undefined}
    >
      {mounted ? (
        // The panel is a (non-modal) dialog: Escape on it closes the dock.
        // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
        <section
          id={panelId}
          className="mn-chat-dock__panel"
          role="dialog"
          aria-label={messages.label}
          aria-hidden={!panelVisible}
          data-visible={panelVisible || undefined}
          onKeyDown={(e) => {
            if (e.key === 'Escape' && layout !== 'fullscreen') {
              e.stopPropagation();
              close();
            }
          }}
        >
          <iframe
            key={frameKey}
            ref={frameRef}
            className="mn-chat-dock__frame"
            src={src}
            title={messages.label}
            sandbox={sandboxValue(sandbox)}
            allow={allowValue(allow)}
            allowFullScreen
            referrerPolicy="strict-origin-when-cross-origin"
            tabIndex={panelVisible ? undefined : -1}
          />
          {!ready ? (
            <div className="mn-chat-dock__loading">
              <Spinner size="md" {...(panelVisible ? { label: messages.loading } : {})} />
            </div>
          ) : null}
        </section>
      ) : null}

      {shown && ring && !panelVisible ? (
        <div className="mn-chat-dock__ring" role="alert">
          <span className="mn-chat-dock__ring-dot" aria-hidden />
          <span className="mn-chat-dock__ring-text">{messages.incomingCall}</span>
          <Button
            size="sm"
            variant="primary"
            onClick={() => openDock({ conversationId: ring.conversationId })}
          >
            {messages.answer}
          </Button>
          <IconButton
            size="sm"
            label={messages.dismiss}
            icon={<CloseGlyph />}
            onClick={() => setRing(null)}
          />
        </div>
      ) : null}

      {shown ? (
        <button
          ref={launcherRef}
          type="button"
          className="mn-chat-dock__launcher"
          aria-label={launcherName}
          title={launcherName}
          aria-expanded={panelVisible}
          aria-controls={mounted ? panelId : undefined}
          onPointerEnter={mountOnIntent ? () => setMounted(true) : undefined}
          onFocus={mountOnIntent ? () => setMounted(true) : undefined}
          onClick={() => (panelVisible ? close() : openDock())}
        >
          <span className="mn-chat-dock__icon" aria-hidden>
            {icon ?? <ChatGlyph />}
          </span>
          {unread > 0 ? (
            <span className="mn-chat-dock__badge" aria-hidden>
              {unread > 99 ? '99+' : unread}
            </span>
          ) : null}
        </button>
      ) : null}
    </div>
  );
});
