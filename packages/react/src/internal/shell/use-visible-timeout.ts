import { useEffect, useRef } from 'react';

/**
 * Calls `onTimeout` once `ms` of *visible* time has passed while `active`.
 * Background tabs throttle timers and delay a frame's messages, so the
 * countdown stops while the page is hidden and starts again, in full, when it
 * becomes visible. Changing `resetKey` restarts it. `ms <= 0` disables it.
 */
export function useVisibleTimeout(
  active: boolean,
  ms: number,
  onTimeout: () => void,
  resetKey?: unknown,
): void {
  const latest = useRef(onTimeout);
  useEffect(() => {
    latest.current = onTimeout;
  });

  useEffect(() => {
    if (!active || !(ms > 0) || typeof document === 'undefined') return;
    let timer: ReturnType<typeof setTimeout> | undefined;
    const arm = () => {
      clearTimeout(timer);
      if (document.visibilityState === 'hidden') return;
      timer = setTimeout(() => latest.current(), ms);
    };
    arm();
    document.addEventListener('visibilitychange', arm);
    return () => {
      clearTimeout(timer);
      document.removeEventListener('visibilitychange', arm);
    };
  }, [active, ms, resetKey]);
}
