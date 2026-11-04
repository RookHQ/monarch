import { useMemo } from 'react';
import { CommandPalette, type CommandPaletteItem } from '@rookcom/monarch-react/data';
import { navigate } from '../../lib/router.js';
import { searchItems } from './search-index.js';

/** The search palette itself; search-palette.tsx loads it on first use. */
export function SearchDialog({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
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
      onOpenChange={onOpenChange}
      hotkey={false}
      items={items}
      className="docs-search"
      messages={{
        title: 'Search the documentation',
        placeholder: 'Search components, patterns and guides…',
        empty: 'Nothing matches. Try a component name, such as “table”.',
      }}
    />
  );
}
