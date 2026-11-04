import {
  SegmentedControl,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
} from '@rookcom/monarch-react';
import type { ContrastPreference } from '@rookcom/monarch-tokens/appearance';
import { products } from '../../lib/registry.js';
import { DEFAULT_PRODUCT } from '../../lib/appearance.js';
import { useAppearance } from './appearance-provider.js';
import { ThemeSwitcher } from './theme-switcher.js';

/** The rows of the appearance popover (appearance-settings.tsx). */
export function AppearancePanel() {
  const { prefs, update } = useAppearance();
  return (
    <>
      <div className="docs-settings__row">
        <span className="docs-settings__label" id="docs-settings-theme">
          Theme
        </span>
        <ThemeSwitcher />
      </div>
      <div className="docs-settings__row">
        <span className="docs-settings__label" id="docs-settings-contrast">
          Contrast
        </span>
        <SegmentedControl<ContrastPreference>
          aria-labelledby="docs-settings-contrast"
          size="sm"
          value={prefs.contrast}
          onValueChange={(contrast) => update({ contrast })}
          options={[
            { value: 'system', label: 'System' },
            { value: 'standard', label: 'Standard' },
            { value: 'more', label: 'More' },
          ]}
        />
      </div>
      <div className="docs-settings__row">
        <span className="docs-settings__label" id="docs-settings-accent">
          Product accent
        </span>
        <Select value={prefs.product} onValueChange={(product) => update({ product })}>
          <SelectTrigger aria-labelledby="docs-settings-accent" size="sm" data-testid="product" />
          <SelectContent>
            <SelectItem value={DEFAULT_PRODUCT}>Rook (default accent)</SelectItem>
            {products.map((p) => (
              <SelectItem key={p.id} value={p.id}>
                {p.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    </>
  );
}
