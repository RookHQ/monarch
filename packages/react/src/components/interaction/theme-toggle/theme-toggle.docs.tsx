import { useState } from 'react';
import {
  ThemeToggle,
  type ShellCookieStore,
  type ThemePreference,
} from '@rookcom/monarch-react/shell';
import type { ComponentDocs } from '../../../internal/component-docs.js';

/** Docs examples keep their choice in memory, so they never change this site's own theme. */
function useDemoCookies(initial?: ThemePreference): ShellCookieStore {
  const [store] = useState<ShellCookieStore>(() => {
    const jar = new Map<string, string>(initial ? [['rook_theme', initial]] : []);
    return { get: (n) => jar.get(n) ?? null, set: (n, v) => void jar.set(n, v) };
  });
  return store;
}

function SettingsDemo() {
  const cookies = useDemoCookies('system');
  return <ThemeToggle cookies={cookies} applyToDocument={false} />;
}

function MenuDemo() {
  const cookies = useDemoCookies('dark');
  return (
    <div style={{ display: 'flex', gap: 'var(--mn-space-3)', alignItems: 'center' }}>
      <ThemeToggle variant="menu" cookies={cookies} applyToDocument={false} />
      <ThemeToggle variant="menu" showLabel={false} cookies={cookies} applyToDocument={false} />
    </div>
  );
}

function ControlledDemo() {
  const [theme, setTheme] = useState<ThemePreference>('light');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)' }}>
      <ThemeToggle
        size="sm"
        value={theme}
        onValueChange={setTheme}
        cookies={null}
        applyToDocument={false}
        messages={{ label: 'Appearance' }}
      />
      <p style={{ margin: 0, color: 'var(--mn-color-text-muted)', fontSize: 'var(--mn-text-sm)' }}>
        Saved preference: {theme}
      </p>
    </div>
  );
}

export default {
  id: 'theme-toggle',
  title: 'ThemeToggle',
  summary:
    'Light, dark or system theme, saved in the shared rook_theme cookie and applied like the pre-paint script.',
  sections: {
    purpose: {
      summary:
        'Lets a person choose the Rook theme once and have every Rook app follow it. The choice lives in the `rook_theme` cookie (on `.rookhq.com` by default) and is applied to `<html>` exactly as `appearanceScript()` from `@rookcom/monarch-tokens` applies it before first paint. Needed by: app, calendar, friends, signal.',
      useWhen: [
        'The theme choice in a rail, an account menu or a toolbar (`variant="menu"`).',
        'An appearance setting on a preferences page (`variant="segmented"`).',
      ],
      avoidWhen: [
        'Previewing a theme inside one panel without changing the app: set `data-mn-theme` on that panel instead.',
        'Public, customer-facing pages that must stay light: scope dark mode with `data-rook-theme-routes` rather than hiding the toggle.',
      ],
    },
    designPrinciples: {
      items: [
        'Three choices, always including System, so the OS setting is respected (one of the two capabilities the registry found missing).',
        'One source of truth: the shared cookie. The toggle never keeps a second copy that could disagree with another app.',
        'Hydration-safe: the server renders `defaultValue`, the client switches to the saved choice through `useSyncExternalStore`, and the pre-paint script has already painted the right theme.',
        'Cookie I/O is injectable (`cookies`), so tests, docs and embedded frames never touch the real cookie.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`applyThemePreference(preference, root?)` applies a preference the same way the toggle does and returns the resolved theme. Use it after reading the cookie on your own.',
        '`ROOK_THEME_COOKIE` (`rook_theme`) and `ROOK_THEME_EVENT` (`rook-theme`, fired on `window` after every change) are the cross-app contract.',
        '`ShellCookieStore` is `{ get(name), set(name, value, { maxAge, domain }) }`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Controlled with `value` + `onValueChange`, or uncontrolled (reads and writes the cookie).',
        '`cookies={null}` keeps the choice in memory for this page only; `applyToDocument={false}` leaves `<html>` alone.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['segmented', 'menu'],
          description:
            '`segmented` shows the three choices as a radio group; `menu` is one button opening a radio menu. Default `segmented`.',
        },
        {
          prop: 'showLabel',
          values: ['true', 'false'],
          description: '`menu` only: show the current choice next to the icon. Default true.',
        },
        { prop: 'size', values: ['sm', 'md'], description: 'Control height. Default `md`.' },
      ],
    },
    states: {
      items: [
        {
          name: 'Current choice',
          attribute: 'data-value="light|dark|system"',
          description:
            'On the root. The segmented choice is `aria-checked`; the menu item is a checked `menuitemradio`, and the trigger names it ("Theme: Dark").',
        },
        {
          name: 'Applied theme',
          attribute: '<html data-mn-theme class="dark" style="color-scheme">',
          description:
            'Written on change. `system` resolves through `prefers-color-scheme` and follows OS changes while the page is open.',
        },
        {
          name: 'Out-of-scope route',
          attribute: '<html data-rook-theme-routes="regex">',
          description: 'Routes that do not match stay light, matching the pre-paint script.',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing saves the cookie for a year, applies the theme to `<html>`, fires `rook-theme` and calls `onValueChange`.',
        'Every toggle on the page re-reads the cookie on `rook-theme`, on window focus and when the tab becomes visible, so a change made in another Rook app shows up when you come back.',
        'The contrast preference (`data-mn-contrast`) is kept as it is.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the selected choice (segmented) or the menu button.' },
        {
          keys: '← → ↑ ↓',
          action: 'Segmented: move between choices and select. Menu: move between items.',
        },
        { keys: 'Enter / Space', action: 'Menu: open the menu, or choose the focused item.' },
        { keys: 'Escape', action: 'Menu: close it and return focus to the button.' },
      ],
    },
    accessibility: {
      role: 'radiogroup (segmented) or a menu button with menuitemradio items (menu)',
      name: '`messages.label` (default "Theme"); the menu button adds the current choice: "Theme: System".',
      items: [
        'Built on SegmentedControl and DropdownMenu from `@rookcom/monarch-react`, which follow the APG radio group and menu button patterns.',
        'Icons are decorative; every choice has a text label, also when the menu button hides its text.',
        'Changing the theme does not move focus.',
      ],
    },
    responsive: {
      items: [
        'The segmented control never grows wider than its container.',
        'In a collapsed rail use `variant="menu"` with `showLabel={false}`; the menu opens to the side you choose (`side="right"`).',
      ],
    },
    theme: {
      items: [
        'Uses the theme of the surface it sits on: tokens only, so light, dark and more-contrast all apply.',
        'Pair with `appearanceScript({ themeCookie: "rook_theme" })` in `<head>` for a flash-free first paint.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Appearance setting',
          render: () => <SettingsDemo />,
          code: `// In <head>: <script dangerouslySetInnerHTML={{ __html: appearanceScript() }} />
<ThemeToggle />`,
        },
        {
          title: 'Menu button, with and without text',
          render: () => <MenuDemo />,
          code: `<ThemeToggle variant="menu" />
<ThemeToggle variant="menu" showLabel={false} side="right" />`,
        },
        {
          title: 'Controlled, translated',
          render: () => <ControlledDemo />,
          code: `const [theme, setTheme] = useState<ThemePreference>('light');

<ThemeToggle
  size="sm"
  value={theme}
  onValueChange={setTheme}
  messages={{ label: 'Appearance' }}
/>`,
        },
      ],
    },
    doDont: {
      do: [
        'Render the tokens pre-paint script in `<head>` of every app that shows the toggle.',
        'Keep the default cookie name and domain in Rook apps, so the choice follows people between apps.',
        'Pass `cookieDomain` for apps on other domains, and a custom `cookies` store in tests.',
      ],
      dont: [
        'Do not read `rook_theme` and toggle `.dark` yourself; call `applyThemePreference` so scope and contrast stay right.',
        'Do not offer only light and dark; System is part of the contract.',
      ],
    },
    related: {
      items: [
        { id: 'rook-rail', reason: 'Has a slot for the theme toggle.' },
        { id: 'segmented-control', reason: 'The control the segmented variant is built on.' },
        { id: 'dropdown-menu', reason: 'The menu the menu variant is built on.' },
        { id: 'brand-theme-engine', reason: 'Product accents, which the theme combines with.' },
      ],
    },
    dependencies: {
      packages: ['@rookcom/monarch-tokens', 'react'],
      components: ['button', 'dropdown-menu', 'segmented-control'],
    },
    testing: {
      unit: [
        'Labelled radio group with the saved choice checked; unknown cookie values fall back to the default.',
        'Choosing writes the cookie (name, one-year max-age, domain), applies the theme to `<html>` and calls `onValueChange`; cookie name and domain are configurable.',
        'Arrow keys move and select; controlled mode follows the value prop only.',
        'Menu variant: the button names the current choice, the menu has checked radio items, choosing applies; hidden text and translated messages.',
        '`system` follows the OS, `data-rook-theme-routes` keeps other routes light, contrast is kept.',
        'Server render uses the default and hydration switches to the saved choice without errors.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/theme-toggle',
        'Cross-browser axe: /components/theme-toggle',
      ],
    },
  },
} satisfies ComponentDocs;
