'use client';

import { useCallback, useEffect, useRef, useState } from 'react';

/** Whether the browser may show system notifications. */
export type AiNotificationPermission = 'default' | 'granted' | 'denied' | 'unsupported';

/** One notification about a background run. */
export interface AiRunNotification {
  /** The run or task id; also the notification tag, so a newer one replaces it. */
  id: string;
  /** e.g. the task title, "Summarise 40 contracts". */
  title: string;
  /** e.g. "Done", "Needs your input" or the error. */
  body?: string | undefined;
}

/** Where a notification went. */
export type AiNotificationDelivery = 'system' | 'in-app' | 'none';

export interface UseAiRunNotificationsOptions {
  /**
   * Only use a system notification while the page is hidden (another tab, a
   * minimised window); while it is visible, use `onInApp`. Default true.
   */
  onlyWhenHidden?: boolean;
  /**
   * The in-app fallback, e.g. a toast: used while the page is visible, and
   * when system notifications are unsupported, not yet allowed or blocked.
   */
  onInApp?: ((notification: AiRunNotification) => void) | undefined;
  /** Called when the person clicks a system notification (the window is focused first). */
  onClick?: ((notification: AiRunNotification) => void) | undefined;
  /** Icon URL for system notifications. */
  icon?: string | undefined;
}

export interface UseAiRunNotificationsResult {
  permission: AiNotificationPermission;
  /**
   * Asks for permission. Call it from a click or key handler: browsers only
   * show the prompt for a user gesture, and never on page load.
   */
  requestPermission: () => Promise<AiNotificationPermission>;
  /** Shows a notification the best available way and says which way it went. */
  notify: (notification: AiRunNotification) => AiNotificationDelivery;
}

type NotificationCtor = {
  permission: NotificationPermission;
  requestPermission: () => Promise<NotificationPermission>;
  new (title: string, options?: NotificationOptions): Notification;
};

function notificationApi(): NotificationCtor | null {
  if (typeof window === 'undefined') return null;
  const api = (window as unknown as { Notification?: NotificationCtor }).Notification;
  return typeof api === 'function' ? api : null;
}

function currentPermission(): AiNotificationPermission {
  const api = notificationApi();
  return api ? api.permission : 'unsupported';
}

/**
 * Notifications for agents working in the background: a system
 * notification when the page is hidden and the person allowed them, an
 * in-app callback (a toast) otherwise. Never asks for permission by itself;
 * call `requestPermission` from a button. Pair it with AiBackgroundTasks
 * (`notify={notifications.notify}`).
 */
export function useAiRunNotifications(
  options: UseAiRunNotificationsOptions = {},
): UseAiRunNotificationsResult {
  // Read after mount, so the server and the first client render agree.
  const [permission, setPermission] = useState<AiNotificationPermission>('unsupported');
  useEffect(() => setPermission(currentPermission()), []);
  const opts = useRef(options);
  opts.current = options;

  const requestPermission = useCallback(async (): Promise<AiNotificationPermission> => {
    const api = notificationApi();
    if (!api) return 'unsupported';
    let next: AiNotificationPermission;
    try {
      next = await api.requestPermission();
    } catch {
      next = api.permission;
    }
    setPermission(next);
    return next;
  }, []);

  const notify = useCallback((n: AiRunNotification): AiNotificationDelivery => {
    const { onlyWhenHidden = true, onInApp, onClick, icon } = opts.current;
    const api = notificationApi();
    const hidden = typeof document !== 'undefined' && document.visibilityState === 'hidden';
    if (api && api.permission === 'granted' && (hidden || !onlyWhenHidden)) {
      try {
        const shown = new api(n.title, {
          tag: n.id,
          ...(n.body ? { body: n.body } : {}),
          ...(icon ? { icon } : {}),
        });
        shown.onclick = () => {
          window.focus();
          shown.close();
          onClick?.(n);
        };
        return 'system';
      } catch {
        // Some browsers allow notifications only from a service worker: fall back.
      }
    }
    if (onInApp) {
      onInApp(n);
      return 'in-app';
    }
    return 'none';
  }, []);

  return { permission, requestPermission, notify };
}
