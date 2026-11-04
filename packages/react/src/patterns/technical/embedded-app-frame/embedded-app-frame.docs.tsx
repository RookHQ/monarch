import { useEffect, useState } from 'react';
import { EmbeddedAppFrame } from '@rookcom/monarch-react/shell';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/**
 * Docs only: a same-origin stand-in for an embedded app page, served from a
 * blob: URL. It copies this page's styles and theme so it looks like a Rook
 * page, then speaks the embed contract (ready + resize) to its parent.
 */
function useDemoPage(body: string, speak: boolean): string | null {
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
    const script = speak
      ? `<script>
const o = location.origin, p = 'rook-app-settings';
const send = (m) => parent.postMessage(Object.assign({ protocol: p, v: 1 }, m), o);
send({ type: 'ready' });
const size = () => { const b = document.body, cs = getComputedStyle(b); send({ type: 'resize', height: Math.ceil(b.getBoundingClientRect().height + parseFloat(cs.marginTop) + parseFloat(cs.marginBottom)) }); };
new ResizeObserver(size).observe(document.body); size();
addEventListener('message', (e) => { if (e.origin === o && e.data && e.data.type === 'ping') send({ type: 'ready' }); });
</script>`
      : '';
    const html = `<!doctype html><html ${attrs}><head><meta charset="utf-8">${head}<style>body{margin:0;padding:var(--mn-space-5);background:var(--mn-color-bg-surface);color:var(--mn-color-text-default);font-family:var(--mn-font-sans);font-size:var(--mn-text-sm)}h2{margin:0 0 var(--mn-space-2);font-size:var(--mn-text-base)}p{margin:0 0 var(--mn-space-3);color:var(--mn-color-text-muted)}label{display:flex;gap:var(--mn-space-2);align-items:center;margin-top:var(--mn-space-2)}</style></head><body>${body}${script}</body></html>`;
    const next = URL.createObjectURL(new Blob([html], { type: 'text/html' }));
    setUrl(next);
    return () => URL.revokeObjectURL(next);
  }, [body, speak]);
  return url;
}

const PAGE = `<h2>Email notifications</h2>
<p>Choose which updates reach your inbox.</p>
<label><input type="checkbox" checked> New tickets assigned to me</label>
<label><input type="checkbox"> Daily summary</label>
<label><input type="checkbox" checked> Mentions</label>`;

function Ready() {
  const src = useDemoPage(PAGE, true);
  if (!src) return null;
  return (
    <div style={{ width: '100%', maxWidth: '36rem' }}>
      <EmbeddedAppFrame
        src={src}
        title="Example Desk: Email notifications"
        allowedOrigins={[window.location.origin]}
        height={160}
        openHref="#open-in-app"
      />
    </div>
  );
}

function Unresponsive() {
  const src = useDemoPage('<h2>Not in embed mode</h2>', false);
  if (!src) return null;
  return (
    <div style={{ width: '100%', maxWidth: '36rem' }}>
      <EmbeddedAppFrame
        src={src}
        title="Example Desk: Billing"
        allowedOrigins={[window.location.origin]}
        readyTimeout={1}
        height={220}
        openHref="#open-in-app"
        messages={{ open: 'Open in Example Desk' }}
      />
    </div>
  );
}

export default {
  id: 'embedded-app-frame',
  title: 'EmbeddedAppFrame',
  summary:
    'Embed a page of another Rook app: sandboxed iframe, ready handshake, auto-height and focus handoff with origin checks.',
  sections: {
    purpose: {
      summary:
        'Rook ID’s App Settings hub shows each app’s settings pages inside its own layout. EmbeddedAppFrame is the host side of that embed contract (v1), and `useEmbedReady` is the embedded side, so every app speaks the same protocol instead of hand-writing postMessage code.',
      useWhen: [
        'Showing a page that belongs to another Rook app inside this one (App Settings, Integrations).',
        'In the embedded app: call `useEmbedReady` in the layout of pages that may be framed.',
      ],
      avoidWhen: [
        'Embedding Rook Chat: use ChatDock, which speaks the `rook-chat-embed` protocol.',
        'Third-party content you do not control: keep the default sandbox and add nothing to `allow`, or link out instead.',
      ],
    },
    designPrinciples: {
      items: [
        'Closed by default: a minimal sandbox (scripts, its own session, forms), no permissions, and messages accepted only from the frame’s own window at an allowed origin.',
        'Never silent: a page that does not say `ready` within 12 seconds of visible time shows an error with Try again and an open-in-app link instead of an endless spinner.',
        'The frame fits its content (auto-height), so the host page scrolls once instead of a box inside a box.',
        'Focus can cross the boundary on purpose: into the page when it is ready, and back out when the page hands it back.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Forwards its ref to the wrapper; `iframeRef` reaches the iframe.',
        'Also exported: `useEmbedReady` (embedded side), `APP_EMBED_PROTOCOL`, `APP_EMBED_VERSION`, `DEFAULT_EMBED_SANDBOX`, `encodeAppEmbedMessage`, `decodeAppEmbedChildMessage`, `decodeAppEmbedHostMessage`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`allowedOrigins` defaults to the origin of `src`. Entries that are not a bare origin (`*`, a path, `null`) are dropped and never match.',
        '`sandbox` takes tokens, not a string; `allow-top-navigation` is not accepted.',
        '`readyTimeout={0}` waits forever (use only when the page shows its own errors).',
      ],
    },
    variants: {
      items: [
        {
          prop: 'autoHeight',
          values: ['true', 'false'],
          description:
            'Follow `resize` messages between `minHeight` and `maxHeight`, or keep the fixed `height`.',
        },
        {
          prop: 'protocol',
          values: ['rook-app-settings', 'custom'],
          description: 'The protocol name in every message, for sibling contracts.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Loading',
          attribute: 'data-status="loading"',
          description: 'A status with a spinner covers the frame until the page says `ready`.',
        },
        {
          name: 'Ready',
          attribute: 'data-status="ready"',
          description: 'The page is shown; `onReady` runs once.',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description:
            'No `ready` in time: an alert with Try again (reloads the frame) and the open-in-app link. The frame is hidden from assistive technology.',
        },
      ],
    },
    interaction: {
      items: [
        'On every load the host posts `ping`, so a page that announced `ready` before the host listened says it again.',
        'Try again remounts the iframe and keeps focus on the loading status instead of losing it to the page.',
        'The countdown pauses while the tab is hidden, since background tabs throttle the embedded page.',
        'A `focus-exit` message moves focus to the control after (forward) or before (backward) the frame, or calls `onFocusExit`.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves into and through the embedded page, then on to the host page.',
        },
        { keys: 'Enter / Space', action: 'Try again and the open link, in the error state.' },
      ],
      notes: [
        'Keys inside the embedded page belong to that page. With `useEmbedReady`, the page can call `exitFocus` (for example on Escape) to hand focus back.',
      ],
    },
    accessibility: {
      role: 'iframe (document) in a wrapper; status while loading, alert on error',
      name: '`title` names the iframe, e.g. "Rook Tickets: Email settings".',
      items: [
        'The loading overlay is a `status`; the error is an `alert` so it is announced when it replaces the spinner.',
        'The open link says it opens in a new tab.',
        'Security: `event.origin` must be in `allowedOrigins` (exact match) and `event.source` must be this frame’s window; each message is validated against its exact shape (`ready` has exactly three keys, `resize` a finite height under 100000), and anything else is dropped.',
        'Security: the host posts only to the origin of `src`, and only when it is allowed; nothing is posted to `*`. `useEmbedReady` posts only to an allowed parent origin (from `ancestorOrigins` or the referrer) and reads messages only from `window.parent` at that origin.',
        'Threat model: a hostile page in another frame or tab cannot spoof `ready` or resize the frame (origin and source checks); a hijacked embedded page cannot navigate the host (no top navigation), open popups, download files or use camera, microphone or clipboard unless the host opts in. `allow-scripts` together with `allow-same-origin` is only safe for cross-origin pages: never point the frame at a page on the host’s own origin with untrusted content.',
      ],
    },
    responsive: {
      items: [
        'The frame is as wide as its container; its height follows the page within `minHeight` and `maxHeight`.',
        'Loading and error content is centred and wraps at 320px.',
      ],
    },
    theme: {
      items: [
        'Surface, border and text tokens for the wrapper and overlays. The embedded page draws itself; Rook apps share the theme cookie, so both sides match.',
        'The height transition is removed under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'An embedded settings page (ready, auto-height)',
          stack: true,
          render: () => <Ready />,
        },
        {
          title: 'A page that never answers',
          stack: true,
          render: () => <Unresponsive />,
        },
      ],
    },
    doDont: {
      do: [
        'List exact origins in `allowedOrigins`, from configuration, per environment.',
        'Announce `ready` only when the embedded page can actually be used (signed in, workspace resolved, or the no-access screen).',
        'Give every frame a specific `title`.',
      ],
      dont: [
        'Do not add `allow-top-navigation`, `allow-popups` or permissions unless the embedded page needs them.',
        'Do not post to `*` or accept `*` as an origin, on either side.',
        'Do not put secrets or tokens in the frame URL.',
      ],
    },
    related: {
      items: [
        { id: 'chat-dock', reason: 'The Rook Chat frame, with its own protocol.' },
        { id: 'support-panel', reason: 'Support chat beside the rail.' },
        {
          id: 'no-app-access',
          reason: 'What the embedded page shows (and announces ready for) without access.',
        },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'spinner', 'visually-hidden'],
    },
    testing: {
      unit: [
        'Titled iframe with the default sandbox, referrer policy and no permissions; loading status.',
        'Ready from the frame’s window at an allowed origin; onReady once.',
        'Drops other origins, look-alike origins, other windows, extra keys, other versions and protocols.',
        '`*` and paths in allowedOrigins never match.',
        'Resize within min and max; fixed height without autoHeight.',
        'Timeout error with Try again (new frame, focus kept) and the new-tab open link.',
        'Ping on load to the page’s own origin; focus on ready; focus-exit forward, backward and onFocusExit.',
        'Unknown sandbox tokens and bad permission names dropped; readyTimeout 0 waits.',
        'Protocol encode/decode both ways; useEmbedReady: inert when not framed, ready to an allowed parent only, ping answered, exitFocus.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/embedded-app-frame',
        'Cross-browser axe: /components/embedded-app-frame',
      ],
    },
  },
} satisfies ComponentDocs;
