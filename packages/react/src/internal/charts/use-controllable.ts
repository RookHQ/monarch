import { useCallback, useRef, useState } from 'react';

/** Controlled when `value` is defined, otherwise internal state seeded from `defaultValue`. */
export function useControllable<T>(
  value: T | undefined,
  defaultValue: T,
  onChange?: (next: T) => void,
): [T, (next: T) => void] {
  const [inner, setInner] = useState<T>(defaultValue);
  const controlled = value !== undefined;
  const current = controlled ? value : inner;
  const latest = useRef(current);
  latest.current = current;
  const set = useCallback(
    (next: T) => {
      if (Object.is(next, latest.current)) return;
      if (!controlled) setInner(next);
      onChange?.(next);
    },
    [controlled, onChange],
  );
  return [current, set];
}

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
