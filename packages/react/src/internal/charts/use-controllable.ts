import { useCallback } from 'react';
import { useControllable } from '../use-controllable.js';

export { useControllable };

/** The hidden-series list behind legend toggles, controlled or uncontrolled. */
export function useHiddenSeries(
  hidden: readonly string[] | undefined,
  defaultHidden: readonly string[] = [],
  onChange?: (hidden: string[]) => void,
): [readonly string[], (key: string) => void] {
  const [current, set] = useControllable<readonly string[]>(
    hidden,
    defaultHidden,
    onChange as ((next: readonly string[]) => void) | undefined,
  );
  const toggle = useCallback(
    (key: string) =>
      set(current.includes(key) ? current.filter((k) => k !== key) : [...current, key]),
    [current, set],
  );
  return [current, toggle];
}
