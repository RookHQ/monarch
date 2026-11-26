import { useState } from 'react';
import { AppearancePanel, type AppearancePreferences } from '@rookcom/monarch-react/shell';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function ControlledDemo() {
  const [prefs, setPrefs] = useState<AppearancePreferences>({
    theme: 'light',
    contrast: 'standard',
    motion: 'system',
    density: 'comfortable',
  });
  return (
    <div style={{ display: 'grid', gap: 8, width: '100%', maxWidth: 560 }}>
      <AppearancePanel
        value={prefs}
        onValueChange={setPrefs}
        cookies={null}
        applyToDocument={false}
        sections={['motion', 'density']}
      />
      <span style={{ fontSize: 'var(--mn-text-sm)' }}>
        Motion: {prefs.motion}, density: {prefs.density}
      </span>
    </div>
  );
}

export default {
  id: 'appearance-panel',
  title: 'AppearancePanel',
  summary:
    'A settings panel for theme, contrast, motion and density, saved and applied the Monarch way.',
  sections: {
    purpose: {
      summary:
        'Every app’s settings page needs the same appearance choices, saved where the pre-paint script can read them so the next page load has no flash. AppearancePanel combines ThemeToggle (the shared `rook_theme` cookie) with contrast (the `mn_contrast` cookie `appearanceScript` already reads), motion and density. Needed by: every Rook frontend’s account or preferences settings.',
      useWhen: [
        'An Appearance or Accessibility section on a settings page.',
        'A preferences popover or sheet where people adjust how the app looks.',
      ],
      avoidWhen: [
        'Only the theme in a rail or toolbar: use ThemeToggle with `variant="menu"`.',
        'Product-specific display options (columns, sort order): build them with SegmentedControl and Switch.',
      ],
    },
    designPrinciples: {
      items: [
        'Saved where it is read: theme and contrast go to the cookies the tokens pre-paint script reads, so the next load is right from the first paint.',
        '“System” is a real choice for theme, contrast and motion, and follows the device while the page is open.',
        'Each row says what the setting does, in one line.',
        'Honest scope: motion and density are applied as `<html data-mn-motion>` / `data-mn-density` attributes, which Monarch CSS does not read yet. Apps can style against them and should save them from `onValueChange`.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `<div class="mn-appearance-panel">` with one `role="group"` row per section (`mn-appearance-panel__row`, `data-section`), each labelled and described by its text.',
        'Theme row: ThemeToggle (segmented). Other rows: SegmentedControl.',
        'Exports `DEFAULT_APPEARANCE` and `MONARCH_CONTRAST_COOKIE` (`mn_contrast`).',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Pass `cookies={null}` for in-memory only (tests, demos), `applyToDocument={false}` to only report choices.',
        'Saved theme and contrast are read after hydration, so the server render and the first client render match.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'sections',
          values: ["['theme', 'contrast', 'motion', 'density']", 'any subset, in any order'],
          description: 'Which rows to show.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Selected option',
          attribute: 'aria-checked="true" on the radio',
          description: 'From SegmentedControl and ThemeToggle.',
        },
        {
          name: 'Applied to the page',
          attribute: 'data-mn-theme, data-mn-contrast, data-mn-motion, data-mn-density on <html>',
          description: 'Set when `applyToDocument` is true.',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing a theme saves `rook_theme` and applies it (ThemeToggle); choosing contrast saves `mn_contrast` and applies it without changing the theme.',
        'Motion “Reduce” sets `data-mn-motion="reduce"`; density “Compact” sets `data-mn-density="compact"`; the other choices remove them.',
        '`onValueChange` receives all four preferences after every change.',
        'Controlled with `value`, or uncontrolled with `defaultValue`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between the rows’ option groups.' },
        {
          keys: 'Arrow keys',
          action: 'Choose the previous or next option in a row (SegmentedControl).',
        },
        { keys: 'Home / End', action: 'First or last option in a row.' },
      ],
    },
    accessibility: {
      role: 'group per row, containing a radiogroup',
      name: 'Each row is named by its visible label and described by its one-line description.',
      items: [
        'Option groups are radio groups with the row label as their name.',
        'No colour-only meaning: every option is text.',
        'Checked with axe in jsdom.',
      ],
    },
    responsive: {
      items: [
        'Text and control sit side by side when there is room and wrap below 480px, where the control takes the full width.',
        'Options keep 44px touch targets on coarse pointers (SegmentedControl).',
      ],
    },
    theme: {
      items: [
        'Rows divided by `--mn-color-border-subtle`; descriptions in `--mn-color-text-muted`.',
        'Forced-colours mode uses CanvasText for dividers and text.',
      ],
    },
    examples: {
      items: [
        {
          title: 'All preferences (kept in memory on this page)',
          stack: true,
          render: () => (
            <div style={{ width: '100%', maxWidth: 560 }}>
              <AppearancePanel cookies={null} heading="Appearance" />
            </div>
          ),
        },
        {
          title: 'Controlled, two sections, not applied',
          stack: true,
          render: () => <ControlledDemo />,
        },
      ],
    },
    doDont: {
      do: [
        'Render the tokens `appearanceScript` in `<head>` so saved choices apply before first paint.',
        'Save motion and density from `onValueChange` and pass them back as `value`.',
      ],
      dont: [
        'Do not ship a second theme switch on the same page.',
        'Do not promise motion or density changes the app does not style yet.',
      ],
    },
    related: {
      items: [
        { id: 'theme-toggle', reason: 'The theme choice alone, for rails and menus.' },
        { id: 'segmented-control', reason: 'The control each row uses.' },
        { id: 'settings-section', reason: 'The settings-page section to place it in.' },
      ],
    },
    dependencies: {
      packages: ['react', '@rookcom/monarch-tokens'],
      components: ['theme-toggle', 'segmented-control'],
    },
    testing: {
      unit: [
        'Four labelled, described rows with named radio groups; axe.',
        'Saved theme and contrast cookies are picked up after mount.',
        'Contrast is saved to mn_contrast and applied, keeping the theme; theme saved and applied through ThemeToggle.',
        'Motion and density attributes set and removed; onValueChange gets all four.',
        'Arrow keys choose options.',
        'Controlled value, chosen sections, title, translation, applyToDocument off.',
        'Server rendering does not touch the document.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
