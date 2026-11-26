import { describe, expect, it } from 'vitest';
import {
  cssClassesOf,
  cssOf,
  defaultFromDescription,
  docsUrlOf,
  fenceIndentedCode,
  importLineOf,
  kebab,
  normaliseDefault,
  pickRootClass,
  portableType,
  preferredEntry,
  scopeOf,
} from './manifest-lib.js';

describe('manifest-lib', () => {
  it('marks only Rook platform exports as rook', () => {
    for (const n of ['RookBar', 'RookRail', 'SignInWithRookButton', 'rookIntelligenceTransport'])
      expect(scopeOf(n)).toBe('rook');
    expect(scopeOf('ROOK_THEME_COOKIE')).toBe('rook');
    for (const n of ['Button', 'AppShell', 'ThemeToggle', 'Brook'])
      expect(scopeOf(n)).toBe('general');
  });

  it('writes exact import lines and stylesheets per entry', () => {
    expect(importLineOf('Button', '.')).toBe("import { Button } from '@rookcom/monarch-react';");
    expect(importLineOf('AiThread', './ai')).toBe(
      "import { AiThread } from '@rookcom/monarch-react/ai';",
    );
    expect(cssOf('.')).toEqual([
      '@rookcom/monarch-tokens/css',
      '@rookcom/monarch-react/styles/core.css',
    ]);
    expect(cssOf('./ai')).toEqual([
      '@rookcom/monarch-tokens/css',
      '@rookcom/monarch-react/styles/core.css',
      '@rookcom/monarch-react/styles/ai.css',
    ]);
    expect(docsUrlOf('button')).toBe('https://monarchui.dev/components/button/');
  });

  it('lists an export under an alias entry first and keeps the old paths after it', () => {
    expect(preferredEntry(['./shell', './rook'], ['./rook'])).toEqual({
      entry: './rook',
      alsoEntries: ['./shell'],
    });
    expect(preferredEntry(['.', './data'], ['./rook'])).toEqual({
      entry: '.',
      alsoEntries: ['./data'],
    });
    expect(preferredEntry(['./ai'], ['./rook'])).toEqual({ entry: './ai' });
    expect(() => preferredEntry([], ['./rook'])).toThrow(/at least one entry/);
    expect(importLineOf('RookRail', './rook')).toBe(
      "import { RookRail } from '@rookcom/monarch-react/rook';",
    );
    expect(cssOf('./rook')).toContain('@rookcom/monarch-react/styles/rook.css');
  });

  it('never leaves a build-machine path in a prop type', () => {
    expect(portableType('import("/home/ci/repo/node_modules/@types/react/index").ReactNode')).toBe(
      'import("react").ReactNode',
    );
    expect(portableType('import("/x/node_modules/csstype/index").Property.Color')).toBe(
      'import("csstype").Property.Color',
    );
    expect(portableType('import("/x/packages/react/src/entries/ai").AiMessage[]')).toBe(
      'import("@rookcom/monarch-react/ai").AiMessage[]',
    );
    expect(portableType('import("/x/packages/react/src/index").ButtonProps')).toBe(
      'import("@rookcom/monarch-react").ButtonProps',
    );
    expect(portableType('import("/x/packages/react/src/components/a/b/b").T')).toBe(
      'import("@rookcom/monarch-react").T',
    );
    expect(portableType('string | number')).toBe('string | number');
  });

  it('kebab-cases export names', () => {
    expect(kebab('SelectTrigger')).toBe('select-trigger');
    expect(kebab('AIBadge')).toBe('ai-badge');
  });

  it('picks a root class that a stylesheet selects', () => {
    const css = cssClassesOf('.mn-input{} .mn-select{} .mn-menu__item{} .mn-button{} .mn-theme{}');
    expect(pickRootClass('SelectTrigger', ['mn-input', 'mn-select'], css)).toBe('mn-select');
    expect(pickRootClass('SelectItem', ['mn-menu__item'], css)).toBe('mn-menu__item');
    expect(pickRootClass('Button', ['mn-theme'], css, ['mn-theme', 'mn-button'])).toBe('mn-button');
    // Template prefixes and classes no stylesheet selects are never reported.
    expect(pickRootClass('Combobox', ['mn-combobox-'], css)).toBeUndefined();
    expect(pickRootClass('Thing', ['mn-thing'], css)).toBeUndefined();
  });

  it('reads defaults from descriptions and normalises them', () => {
    expect(defaultFromDescription('Visual weight. Default `secondary`.')).toBe('secondary');
    expect(defaultFromDescription('Defaults to `md`.')).toBe('md');
    expect(defaultFromDescription('No default here.')).toBeUndefined();
    expect(normaliseDefault('"md"')).toBe("'md'");
    expect(normaliseDefault('automatic', ['manual', 'automatic'])).toBe("'automatic'");
    expect(normaliseDefault('4')).toBe('4');
  });

  it('fences indented doc-comment examples', () => {
    const text = 'Select.\n\n  <Select>\n    <SelectItem />\n  </Select>\n\nAfter.';
    expect(fenceIndentedCode(text)).toBe(
      'Select.\n\n```tsx\n<Select>\n  <SelectItem />\n</Select>\n```\n\nAfter.',
    );
    expect(fenceIndentedCode('Plain text.')).toBe('Plain text.');
  });
});
