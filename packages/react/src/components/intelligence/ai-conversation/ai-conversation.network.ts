/**
 * Whether the device is online, as `useAiConversation` sees it: a small
 * interface so the browser's signal can be replaced in tests, in a native
 * shell, or by a health check that knows more than `navigator.onLine`.
 */

/** Says whether the device is online and tells listeners when that changes. */
export interface AiNetworkSource {
  /** Whether the device is online now. */
  isOnline(): boolean;
  /** Calls `listener` with the new value on every change; returns the unsubscribe function. */
  subscribe(listener: (online: boolean) => void): () => void;
}

/**
 * The browser's signal: `navigator.onLine` and the window's `online` and
 * `offline` events. On a server it is always online. `navigator.onLine` only
 * knows that there is no network at all; a connection that exists but reaches
 * nothing still reads as online, and those failures arrive as `network` errors.
 */
export function browserNetworkSource(): AiNetworkSource {
  return {
    isOnline: () => (typeof navigator === 'undefined' ? true : navigator.onLine !== false),
    subscribe(listener) {
      if (typeof window === 'undefined') return () => undefined;
      const online = () => listener(true);
      const offline = () => listener(false);
      window.addEventListener('online', online);
      window.addEventListener('offline', offline);
      return () => {
        window.removeEventListener('online', online);
        window.removeEventListener('offline', offline);
      };
    },
  };
}

/** A network source you set by hand, for tests and stories. */
export interface AiManualNetworkSource extends AiNetworkSource {
  /** Goes online or offline and tells the listeners. */
  set(online: boolean): void;
}

/** A network source driven by `set(online)`, for tests and stories. Starts online unless told otherwise. */
export function manualNetworkSource(initial = true): AiManualNetworkSource {
  let online = initial;
  const listeners = new Set<(online: boolean) => void>();
  return {
    isOnline: () => online,
    subscribe(listener) {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    set(next) {
      if (next === online) return;
      online = next;
      listeners.forEach((listener) => listener(next));
    },
  };
}
