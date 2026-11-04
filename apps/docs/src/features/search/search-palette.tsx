import { useMemo, useState } from 'react';
import { Kbd } from '@rookcom/monarch-react';
import { CommandPalette, formatHotkey, type CommandPaletteItem } from '@rookcom/monarch-react/data';
import { navigate } from '../../lib/router.js';
import { SearchIcon } from '../../ui/icons.js';
import { searchItems } from './search-index.js';

/**
 * Site search: Monarch's own CommandPalette over every docs page, component
 * and pattern (search-index.ts). Opens with ⌘K / Ctrl K or the top-bar button.
 */
export function SearchPalette() {
  const [open, setOpen] = useState(false);
  const keys = useMemo(() => formatHotkey('mod+k'), []);
  const items: CommandPaletteItem[] = useMemo(
    () =>
      searchItems().map((entry) => ({
        ...entry,
        onSelect: () => navigate(entry.path),
      })),
    [],
  );
  return (
    <CommandPalette
      open={open}
      onOpenChange={setOpen}
      items={items}
      className="docs-search"
      messages={{
        title: 'Search the documentation',
        placeholder: 'Search components, patterns and guides…',
        empty: 'Nothing matches. Try a component name, such as “table”.',
      }}
      trigger={
        <button type="button" className="docs-search-trigger" aria-keyshortcuts="Meta+K Control+K">
          <SearchIcon className="docs-search-trigger__icon" />
          <span className="docs-search-trigger__label">Search</span>
          <span className="docs-search-trigger__keys" aria-hidden>
            <Kbd>{keys.join(keys[0]?.length === 1 ? '' : ' ')}</Kbd>
          </span>
        </button>
      }
    />
  );
}
