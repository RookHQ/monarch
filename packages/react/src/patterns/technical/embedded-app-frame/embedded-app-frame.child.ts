'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { exactOrigins } from '../../../internal/shell/embed.js';
import { tabbables } from '../../../internal/shell/focus.js';
import {
  APP_EMBED_PROTOCOL,
  decodeAppEmbedHostMessage,
  encodeAppEmbedMessage,
  type AppEmbedChildMessage,
  type EmbedFocusDirection,
} from './embedded-app-frame.protocol.js';

export interface UseEmbedReadyOptions {
  /**
   * Exact origins allowed to embed this page (the host apps). Nothing is
   * posted, and no message is read, unless the parent is one of them.
   */
  allowedParentOrigins: readonly string[];
  /** Announce `ready` once this is true (signed in, workspace resolved). Default true. */
  ready?: boolean | undefined;
  /** Report the content height so the host frame fits it. Default true. */
  autoHeight?: boolean | undefined;
  /** Protocol name. Default `rook-app-settings`. */
  protocol?: string | undefined;
  /**
   * The host asked to move focus into the page. Default: the first (forward)
   * or last (backward) tabbable element of the document.
   */
  onFocusRequest?: ((direction: EmbedFocusDirection) => void) | undefined;
}

export interface UseEmbedReadyResult {
  /** True once the page knows it is framed by an allowed host. False on the server. */
  embedded: boolean;
  /** The host's origin, when embedded. */
  parentOrigin: string | null;
  /** Hand focus back to the host page (e.g. on Escape, or Tab past the last control). */
  exitFocus: (direction: EmbedFocusDirection) => void;
}

function parentOriginCandidate(): string | null {
  try {
    const ancestors = window.location.ancestorOrigins;
    if (ancestors && ancestors.length > 0) return ancestors[0] ?? null;
    if (document.referrer) return new URL(document.referrer).origin;
  } catch {
    /* unknown parent */
  }
  return null;
}

/**
 * The embedded side of the App Settings contract: call it in the layout of a
 * page that may be shown inside EmbeddedAppFrame. It announces `ready` (and
 * again on `ping`), reports the content height while `autoHeight`, answers
 * `focus` requests, and returns `exitFocus` to hand focus back. It talks only
 * to `window.parent` at an allowed origin and never posts to `*`.
 */
export function useEmbedReady(options: UseEmbedReadyOptions): UseEmbedReadyResult {
  const {
    allowedParentOrigins,
    ready = true,
    autoHeight = true,
    protocol = APP_EMBED_PROTOCOL,
  } = options;
  const allowedKey = allowedParentOrigins.join('\n');
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const allowed = useMemo(() => exactOrigins(allowedParentOrigins), [allowedKey]);
  const [parentOrigin, setParentOrigin] = useState<string | null>(null);
  const onFocusRequest = useRef(options.onFocusRequest);
  useEffect(() => {
    onFocusRequest.current = options.onFocusRequest;
  });

  useEffect(() => {
    if (window.parent === window) return;
    const candidate = parentOriginCandidate();
    setParentOrigin(candidate && allowed.has(candidate) ? candidate : null);
  }, [allowed]);

  const send = useCallback(
    (message: AppEmbedChildMessage) => {
      if (!parentOrigin) return;
      try {
        window.parent.postMessage(encodeAppEmbedMessage(message, protocol), parentOrigin);
      } catch {
        /* never break the page */
      }
    },
    [parentOrigin, protocol],
  );

  const readyRef = useRef(ready);
  readyRef.current = ready;

  useEffect(() => {
    if (parentOrigin && ready) send({ type: 'ready' });
  }, [parentOrigin, ready, send]);

  useEffect(() => {
    if (!parentOrigin) return;
    const onMessage = (e: MessageEvent) => {
      if (e.source !== window.parent || e.origin !== parentOrigin) return;
      const m = decodeAppEmbedHostMessage(e.data, protocol);
      if (!m) return;
      if (m.type === 'ping') {
        if (readyRef.current) send({ type: 'ready' });
      } else if (onFocusRequest.current) {
        onFocusRequest.current(m.direction);
      } else {
        const list = tabbables(document);
        (m.direction === 'forward' ? list[0] : list[list.length - 1])?.focus();
      }
    };
    window.addEventListener('message', onMessage);
    return () => window.removeEventListener('message', onMessage);
  }, [parentOrigin, protocol, send]);

  useEffect(() => {
    if (!parentOrigin || !autoHeight || typeof ResizeObserver === 'undefined') return;
    let last = -1;
    let frame = 0;
    const report = () => {
      frame = 0;
      // The body's own height (not the document's scrollHeight, which never
      // drops below the frame's viewport), so the frame can shrink too.
      const body = document.body;
      const cs = getComputedStyle(body);
      const h = Math.ceil(
        body.getBoundingClientRect().height +
          (parseFloat(cs.marginTop) || 0) +
          (parseFloat(cs.marginBottom) || 0),
      );
      if (h !== last) {
        last = h;
        send({ type: 'resize', height: h });
      }
    };
    const observer = new ResizeObserver(() => {
      if (!frame) frame = requestAnimationFrame(report);
    });
    observer.observe(document.body);
    report();
    return () => {
      observer.disconnect();
      if (frame) cancelAnimationFrame(frame);
    };
  }, [parentOrigin, autoHeight, send]);

  const exitFocus = useCallback(
    (direction: EmbedFocusDirection) => send({ type: 'focus-exit', direction }),
    [send],
  );

  return { embedded: parentOrigin !== null, parentOrigin, exitFocus };
}
