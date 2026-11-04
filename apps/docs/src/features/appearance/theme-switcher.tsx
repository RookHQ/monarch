import { SegmentedControl } from '@rookcom/monarch-react';
import type { ThemePreference } from '@rookcom/monarch-tokens/appearance';
import { MonitorIcon, MoonIcon, SunIcon } from '../../ui/icons.js';
import { useAppearance } from './appearance-provider.js';

const OPTIONS = [
  { value: 'system', label: 'System', icon: <MonitorIcon /> },
  { value: 'light', label: 'Light', icon: <SunIcon /> },
  { value: 'dark', label: 'Dark', icon: <MoonIcon /> },
] as const;

/**
 * System / light / dark as one segmented control. Saves the shared `rook_theme`
 * cookie, so the choice follows the visitor into every Rook app.
 */
export function ThemeSwitcher({
  compact = false,
  className,
}: {
  /** Icons only; the label stays the accessible name and tooltip. */
  compact?: boolean;
  className?: string;
}) {
  const { prefs, update } = useAppearance();
  return (
    <SegmentedControl<ThemePreference>
      aria-label="Theme"
      className={className}
      size="sm"
      value={prefs.theme}
      onValueChange={(theme) => update({ theme })}
      options={OPTIONS.map((o) => ({ ...o, hideLabel: compact }))}
    />
  );
}
