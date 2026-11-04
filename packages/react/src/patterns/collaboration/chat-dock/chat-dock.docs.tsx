import { useEffect, useState, type CSSProperties, type ReactNode } from 'react';
import { ChatDock } from '@rookcom/monarch-react/shell';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/**
 * Docs only: a same-origin stand-in for the Chat embed, served from a blob:
 * URL with this page's styles. It says `ready` and reports an unread count,
 * like the real embed does.
 */
function useDemoEmbed(): string | null {
  const [url, setUrl] = useState<string | null>(null);
  useEffect(() => {
    const head = Array.from(document.head.querySelectorAll('style, link[rel="stylesheet"]'))
      // Absolute hrefs: a blob: document cannot resolve this page's relative URLs.
      .map((n) =>
        n instanceof HTMLLinkElement ? `<link rel="stylesheet" href="${n.href}">` : n.outerHTML,
      )
      .join('');
    const attrs = Array.from(document.documentElement.attributes)
      .map((a) => `${a.name}="${a.value.replace(/"/g, '&quot;')}"`)
      .join(' ');
    const rows = ['Design review', 'Launch planning', 'Ada Lovelace']
      .map(
        (t, i) =>
          `<li><strong>${t}</strong><span>${['Notes are in the doc.', 'Moving the date to Friday.', 'Thanks!'][i]}</span></li>`,
      )
      .join('');
    const html = `<!doctype html><html ${attrs}><head><meta charset="utf-8">${head}<style>body{margin:0;background:var(--mn-color-bg-surface);color:var(--mn-color-text-default);font-family:var(--mn-font-sans);font-size:var(--mn-text-sm)}h2{margin:0;padding:var(--mn-space-4);font-size:var(--mn-text-base);border-bottom:1px solid var(--mn-color-border-subtle)}ul{list-style:none;margin:0;padding:0}li{display:flex;flex-direction:column;gap:2px;padding:var(--mn-space-3) var(--mn-space-4);border-bottom:1px solid var(--mn-color-border-subtle)}span{color:var(--mn-color-text-muted)}</style></head><body><h2>Chat</h2><ul>${rows}</ul><script>
const send = (m) => parent.postMessage(Object.assign({ protocol: 'rook-chat-embed', v: 1 }, m), location.origin);
send({ type: 'rook-chat:ready' });
send({ type: 'rook-chat:unread', count: 3 });
</script></body></html>`;
    const next = URL.createObjectURL(new Blob([html], { type: 'text/html' }));
    setUrl(next);
    return () => URL.revokeObjectURL(next);
  }, []);
  return url;
}

/** A transformed box contains the dock's fixed positioning, so it stays in the example. */
function Stage({ children }: { children: ReactNode }) {
  return (
    <div
      style={
        {
          position: 'relative',
          width: '100%',
          height: '30rem',
          overflow: 'hidden',
          transform: 'translateZ(0)',
          border: '1px solid var(--mn-color-border-default)',
          borderRadius: 'var(--mn-radius-lg)',
          background: 'var(--mn-color-bg-canvas)',
          '--mn-chat-dock-height': '22rem',
          '--mn-chat-dock-width': 'min(22rem, calc(100% - 2.5rem))',
        } as CSSProperties
      }
    >
      <p style={{ margin: 'var(--mn-space-5)', color: 'var(--mn-color-text-muted)' }}>
        The page of the host app.
      </p>
      {children}
    </div>
  );
}

function Basic() {
  const src = useDemoEmbed();
  return (
    <Stage>
      {src ? (
        <ChatDock
          src={src}
          allowedOrigins={[window.location.origin]}
          appName="Example Desk"
          workspaceId="ws_demo"
          refreshSession={() => Promise.resolve()}
          unread={3}
        />
      ) : null}
    </Stage>
  );
}

export default {
  id: 'chat-dock',
  title: 'ChatDock',
  summary:
    'The Rook Chat launcher with an unread badge and the panel that holds Rook Chat in an iframe (rook-chat-embed v1).',
  sections: {
    purpose: {
      summary:
        'Every Rook app (except Rook Chat itself) docks Rook Chat in the bottom-right corner. The panel is the real Chat app in an iframe, so the host only draws the launcher and speaks the `rook-chat-embed` v1 protocol. ChatDock is the single shared implementation.',
      useWhen: [
        'Once per signed-in app layout, when the person has Chat in this workspace.',
        'With `hidden` on pages that have their own AI assistant on the right.',
      ],
      avoidWhen: [
        'Help from the Rook team: that is SupportPanel, opened from the rail, never bottom-right.',
        'Embedding another app’s settings page: use EmbeddedAppFrame.',
      ],
    },
    designPrinciples: {
      items: [
        'The host never fetches: the product passes the unread count it already polls and its own single-flight `refreshSession`; Chat asks for refreshes and the dock forwards them.',
        'Never refreshes on its own: the frame is created on first open (or hover) and kept alive while closed, so calls, drafts and scroll position survive; it reloads only when `src` or the workspace changes.',
        'Strict messaging: messages are read only from the frame’s own window at an allowed origin, validated field by field, and posted only to the embed’s exact origin.',
        'Gets out of the way: `hidden` hides it where an assistant sits; a running call stays on screen.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the root element. `controllerRef` gives `open(detail)`, `close()` and `post(message)` for "Share in Chat" buttons.',
        'Also exported: `CHAT_EMBED_PROTOCOL`, `CHAT_EMBED_VERSION`, `encodeChatEmbedMessage`, `decodeChatEmbedMessage`, `createChatDockBridge` (framework-free host bridge), `chatPageContext`, `cleanOpenDetail`, `DEFAULT_CHAT_SANDBOX`, `DEFAULT_CHAT_ALLOW`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`allowedOrigins` is required: exact origins only (`*` and paths are dropped). The origin of `src` must be in it or nothing is posted.',
        '`unread` is the count before the frame runs; once the frame reports `rook-chat:unread`, its count wins.',
        '`open` + `onOpenChange` control the panel; `defaultOpen` leaves it uncontrolled.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'layout (from the frame)',
          values: ['panel', 'expanded', 'call', 'fullscreen'],
          description:
            'Chat asks for a size with `rook-chat:layout`; exposed as `data-layout`. Fullscreen hides the launcher and ignores Escape on the host.',
        },
        {
          prop: 'hidden',
          values: ['false', 'true'],
          description: 'Hides launcher and panel; the frame stays connected.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Closed', description: 'The launcher only (no frame until it is wanted).' },
        {
          name: 'Unread',
          description:
            'A badge with the count (99+ above 99), hidden from assistive technology; the count is in the launcher’s name.',
        },
        {
          name: 'Loading',
          description: 'The panel shows a spinner until the frame says `rook-chat:ready`.',
        },
        {
          name: 'Open',
          attribute: 'data-open',
          description: 'The panel is shown and the launcher is `aria-expanded="true"`.',
        },
        {
          name: 'Ringing',
          description:
            'An incoming call while closed: an alert with Open and Dismiss, gone after 30 seconds.',
        },
        {
          name: 'Hidden / unavailable',
          attribute: 'data-hidden',
          description: '`hidden`, or Chat reported `unavailable`: nothing on screen, frame kept.',
        },
      ],
    },
    interaction: {
      items: [
        'Hover or focus on the launcher creates the frame, so opening feels instant (`mountOnIntent`).',
        'Opening moves focus into the frame; closing (launcher, Escape on the panel, or Chat’s own minimise) returns it to the launcher.',
        'When the frame says ready, the dock sends `init` and the page context first, then anything queued (such as an `open` for a conversation).',
        'The frame is told when it is shown or hidden (`rook-chat:visibility`).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'Open or close the panel from the launcher.' },
        { keys: 'Escape', action: 'Close the panel when focus is on it (not in fullscreen).' },
        { keys: 'Tab', action: 'From the launcher into the open panel’s frame.' },
      ],
      notes: [
        'Inside the frame, keys belong to Rook Chat; its own minimise sends `rook-chat:close` and focus returns to the launcher.',
      ],
    },
    accessibility: {
      role: 'button (launcher) controlling a non-modal dialog that holds an iframe',
      name: 'The launcher is "Rook Chat" or "Rook Chat, 3 unread" (`messages`); the panel and the iframe are named "Rook Chat".',
      items: [
        'The launcher has `aria-expanded` and `aria-controls`; the badge is `aria-hidden` because the count is in the name.',
        'The kept-alive panel is `aria-hidden` and out of the tab order while closed.',
        'The incoming call is a `role="alert"` with labelled Open and Dismiss buttons.',
        'Security: `event.origin` must be in `allowedOrigins` (exact) and `event.source` the frame’s window; every message is decoded against its exact shape (ids `[A-Za-z0-9_-]{1,64}`, counts integers under a million, known layouts and statuses) and dropped otherwise. Host messages to the frame go only to the origin of `src`, never to `*`.',
        'Security: `open` details are cleaned before they are sent (bad ids and non-http URLs are dropped). Refresh requests are answered only with the host’s own `refreshSession` result (ok or not), never with a token.',
        'Threat model: another frame or tab cannot drive the badge, layout or refresh (origin and source checks). The frame is sandboxed without top navigation; it gets camera, microphone, screen capture and clipboard for calls (`DEFAULT_CHAT_ALLOW`), so only point `src` at Rook Chat.',
      ],
    },
    responsive: {
      items: [
        'The panel is 25rem wide and up to 40rem tall, never larger than the screen; below 30rem it spans the width above the launcher.',
        'Expanded and call layouts grow to 67.5rem; fullscreen covers the viewport. Offsets respect safe-area insets.',
        'Size and offset can be tuned with `--mn-chat-dock-width`, `--mn-chat-dock-height` and `--mn-chat-dock-offset`.',
      ],
    },
    theme: {
      items: [
        'The launcher uses the inverse surface (dark on light, light on dark) with a border so it stays visible on either; the badge uses the danger solid colour.',
        'Hover lift, panel transitions and the ring pulse are removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Docked, with three unread',
          stack: true,
          render: () => <Basic />,
          code: `<ChatDock
  src={\`\${CHAT_ORIGIN}/embed/dock?ws=\${workspace.id}\`}
  allowedOrigins={[CHAT_ORIGIN]}
  appName="Example Desk"
  workspaceId={workspace.id}
  refreshSession={refreshSession}
  unread={unreadFromPoll}
  hidden={pageHasAssistant}
/>`,
        },
      ],
    },
    doDont: {
      do: [
        'Render one dock in the signed-in app layout. Switching workspace changes `workspaceId`, and the dock starts a fresh frame for it.',
        'Pass the app’s single-flight session refresh, the same one the rest of the app uses.',
        'Set `hidden` on pages with an assistant panel on the right.',
      ],
      dont: [
        'Do not remount the dock on navigation; that would end calls.',
        'Do not refresh the session from the frame or on a timer for it.',
        'Do not put the support chat here: Help & support lives in the rail.',
      ],
    },
    related: {
      items: [
        { id: 'support-panel', reason: 'Help from the Rook team, from the rail.' },
        { id: 'embedded-app-frame', reason: 'Embedding other Rook app pages.' },
        { id: 'rook-rail', reason: 'The rest of the app chrome.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'spinner'],
    },
    testing: {
      unit: [
        'Named launcher, no frame until wanted; mount on hover/focus, or not with mountOnIntent off.',
        'Opens a sandboxed dialog panel, focus into the frame; Escape closes and returns focus.',
        'The frame is kept across close and reopen (never reloads on its own).',
        'Handshake: init and context to the exact origin, then queued messages; visibility.',
        'Drops other origins, look-alikes, other windows, bad shapes, other versions and host-only types.',
        'Unread badge in the name, 99+, follows the frame.',
        'Refresh requests answered with the host refresh (ok and failed); never called on its own.',
        'hidden keeps the frame; a call stays visible; close from inside; unavailable hides.',
        'Incoming call alert opens the conversation; unsafe open details dropped; controlled; translation.',
        'Protocol decoding; the bridge never posts outside the allow list.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/chat-dock',
        'Cross-browser axe: /components/chat-dock',
      ],
    },
  },
} satisfies ComponentDocs;
