import { useSyncExternalStore } from 'react';
import { todayIso, type IsoDate } from './iso.js';

/** Re-renders subscribers just after local midnight, so "today" never goes stale on an open page. */
function subscribeMidnight(onChange: () => void): () => void {
  let timer: ReturnType<typeof setTimeout> | undefined;
  const arm = () => {
    const now = new Date();
    const next = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
    timer = setTimeout(
      () => {
        onChange();
        arm();
      },
      next.getTime() - now.getTime() + 50,
    );
  };
  arm();
  return () => clearTimeout(timer);
}

const localToday = () => todayIso();
// The server's time zone is not the viewer's. The server and the hydrating
// browser both render the UTC date, then the browser switches to its own.
const serverToday = () => todayIso({ timeZone: 'UTC' });

/**
 * Today's date in the viewer's time zone, kept current across midnight.
 * `override` (a prop such as `today`) wins, for tests, demos and apps that
 * use a workspace time zone rather than the device's.
 */
export function useToday(override?: IsoDate | null): IsoDate {
  const live = useSyncExternalStore(subscribeMidnight, localToday, serverToday);
  return override ?? live;
}
