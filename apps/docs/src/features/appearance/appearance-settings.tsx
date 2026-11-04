import { Suspense, lazy } from 'react';
import {
  IconButton,
  Popover,
  PopoverContent,
  PopoverTrigger,
  Tooltip,
} from '@rookcom/monarch-react';
import { SlidersIcon } from '../../ui/icons.js';

/** The panel (and its product Select) loads when the popover first opens, or on hover or focus. */
const loadPanel = () => import('./appearance-panel.js');
const AppearancePanel = lazy(() => loadPanel().then((m) => ({ default: m.AppearancePanel })));

/**
 * Theme, contrast and product accent in one small popover. The accent preview
 * lets any example be seen as any Rook product would show it.
 */
export function AppearanceSettings() {
  return (
    <Popover>
      <Tooltip content="Appearance">
        <PopoverTrigger asChild>
          <IconButton
            label="Appearance settings"
            icon={<SlidersIcon />}
            variant="ghost"
            size="sm"
            data-testid="appearance"
            onPointerEnter={() => void loadPanel()}
            onFocus={() => void loadPanel()}
          />
        </PopoverTrigger>
      </Tooltip>
      <PopoverContent aria-label="Appearance" className="docs-settings" align="end">
        <Suspense fallback={<div className="docs-settings__loading" aria-hidden />}>
          <AppearancePanel />
        </Suspense>
      </PopoverContent>
    </Popover>
  );
}
