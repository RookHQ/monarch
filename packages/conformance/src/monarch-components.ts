/**
 * Public components shipped by @rookcom/monarch-react. A local file with one of
 * these stems under components/ui (or primitives/common) is a duplicate.
 * Keep in sync with packages/react/src/index.ts.
 */
export const MONARCH_COMPONENTS = [
  { name: 'Button', stem: 'button' },
  { name: 'IconButton', stem: 'icon-button' },
  { name: 'Spinner', stem: 'spinner' },
  { name: 'VisuallyHidden', stem: 'visually-hidden' },
  { name: 'Kbd', stem: 'kbd' },
  { name: 'Field', stem: 'field' },
  { name: 'Input', stem: 'input' },
  { name: 'Textarea', stem: 'textarea' },
  { name: 'Checkbox', stem: 'checkbox' },
  { name: 'Switch', stem: 'switch' },
  { name: 'Select', stem: 'select' },
  { name: 'Badge', stem: 'badge' },
  { name: 'Avatar', stem: 'avatar' },
  { name: 'Card', stem: 'card' },
  { name: 'Skeleton', stem: 'skeleton' },
  { name: 'EmptyState', stem: 'empty-state' },
  { name: 'Alert', stem: 'alert' },
  { name: 'Dialog', stem: 'dialog' },
  { name: 'DropdownMenu', stem: 'dropdown-menu' },
  { name: 'Tooltip', stem: 'tooltip' },
  { name: 'Popover', stem: 'popover' },
  { name: 'Tabs', stem: 'tabs' },
  { name: 'Toast', stem: 'toast' },
  { name: 'Table', stem: 'table' },
  { name: 'Breadcrumb', stem: 'breadcrumb' },
  { name: 'Pagination', stem: 'pagination' },
  { name: 'Combobox', stem: 'combobox' },
  { name: 'Separator', stem: 'separator' },
  { name: 'Progress', stem: 'progress' },
  { name: 'Link', stem: 'link' },
  { name: 'PageHeader', stem: 'page-header' },
  { name: 'Toolbar', stem: 'toolbar' },
  { name: 'FilterBar', stem: 'filter-bar' },
  { name: 'SettingsSection', stem: 'settings' },
  { name: 'DescriptionList', stem: 'description-list' },
  { name: 'SplitLayout', stem: 'split-layout' },
  { name: 'StatCard', stem: 'stat-card' },
  { name: 'ErrorState', stem: 'states' },
] as const;

export const MONARCH_COMPONENT_STEMS: ReadonlyMap<string, string> = new Map(
  MONARCH_COMPONENTS.map((c) => [c.stem, c.name]),
);
