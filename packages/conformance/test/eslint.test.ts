import { mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { Linter, RuleTester, type ESLint, type Rule } from 'eslint';
import tseslint from 'typescript-eslint';
import { afterAll, describe, expect, it } from 'vitest';
import { COLOUR_TOKENS } from '../src/bundled-data.js';
import plugin, { RECOMMENDED_LEVELS, configs, rules } from '../src/eslint/index.js';
import { scaleClass } from '../src/eslint/rules/no-arbitrary-spacing.js';
import { alternativeFor } from '../src/eslint/rules/no-rook-specific.js';
import { moduleOfComponent } from '../src/eslint/rules/prefer-monarch-component.js';
import { configuredStyles } from '../src/eslint/rules/require-entry-styles.js';
import { MONARCH_COMPONENTS } from '../src/monarch-components.js';

RuleTester.describe = describe;
RuleTester.it = it;
RuleTester.itOnly = it.only;

/** The plugin is a valid ESLint plugin by type as well as at run time. */
const asEslintPlugin: ESLint.Plugin = plugin;
const asEslintConfig: Linter.Config = configs.recommended;
const asEslintRule: Rule.RuleModule = rules['no-deep-imports'];

const jsx = new RuleTester({
  languageOptions: {
    ecmaVersion: 2024,
    sourceType: 'module',
    parserOptions: { ecmaFeatures: { jsx: true } },
  },
});
const ts = new RuleTester({
  languageOptions: {
    parser: tseslint.parser,
    parserOptions: { ecmaFeatures: { jsx: true } },
  },
});

const [themedHex, themedToken] = Object.entries(COLOUR_TOKENS).find(([, t]) => !t.invariant) ?? [
  '',
  { variable: '' },
];

describe('plugin', () => {
  it('exports every rule with docs, messages and a schema', () => {
    expect(Object.keys(rules).sort()).toEqual(
      [
        'no-arbitrary-spacing',
        'no-deep-imports',
        'no-deprecated-api',
        'no-raw-colors',
        'no-rook-specific',
        'prefer-monarch-component',
        'require-accessible-name',
        'require-entry-styles',
      ].sort(),
    );
    for (const [name, rule] of Object.entries(rules)) {
      expect(rule.meta.docs.description.length).toBeGreaterThan(20);
      expect(rule.meta.docs.url).toContain(encodeURIComponent(`monarch/${name}`));
      expect(Object.keys(rule.meta.messages).length).toBeGreaterThan(0);
      expect(Array.isArray(rule.meta.schema)).toBe(true);
    }
    expect(asEslintPlugin.meta?.name).toBe('@rookcom/monarch-conformance/eslint');
    expect(asEslintConfig.name).toBe('monarch/recommended');
    expect(asEslintRule.meta?.fixable).toBe('code');
  });

  it('marks fixable rules and rules with suggestions', () => {
    expect(rules['no-deep-imports'].meta.fixable).toBe('code');
    expect(rules['no-deprecated-api'].meta.fixable).toBe('code');
    expect(rules['no-raw-colors'].meta.hasSuggestions).toBe(true);
    expect(rules['no-arbitrary-spacing'].meta.hasSuggestions).toBe(true);
  });

  it('recommended leaves no-rook-specific off; general turns it on', () => {
    expect(configs.recommended.rules).toEqual(
      Object.fromEntries(Object.entries(RECOMMENDED_LEVELS).map(([k, v]) => [`monarch/${k}`, v])),
    );
    expect(configs.recommended.rules['monarch/no-rook-specific']).toBe('off');
    expect(configs.general.rules['monarch/no-rook-specific']).toBe('warn');
    expect(configs.general.rules['monarch/no-raw-colors']).toBe('error');
    expect(configs.recommended.plugins.monarch).toBe(plugin);
    expect(plugin.configs.general).toBe(configs.general);
    expect(RECOMMENDED_LEVELS['no-rook-specific']).not.toBe(
      plugin.configs.general.rules['monarch/no-rook-specific'],
    );
  });

  it('runs as a flat config in ESLint', () => {
    const linter = new Linter({ configType: 'flat' });
    const messages = linter.verify(
      `import { RookRail } from '@rookcom/monarch-react/dist/entries/shell.js';\nexport const a = <div className="bg-red-500 p-[7px]" />;\n`,
      [
        {
          ...configs.general,
          languageOptions: { parserOptions: { ecmaFeatures: { jsx: true } } },
        },
      ],
      { filename: 'a.jsx' },
    );
    expect(messages.map((m) => m.ruleId).sort()).toEqual([
      'monarch/no-arbitrary-spacing',
      'monarch/no-deep-imports',
      'monarch/no-raw-colors',
      'monarch/no-rook-specific',
    ]);
    expect(messages.find((m) => m.ruleId === 'monarch/no-raw-colors')?.severity).toBe(2);
    expect(messages.find((m) => m.ruleId === 'monarch/no-arbitrary-spacing')?.severity).toBe(1);
  });
});

jsx.run('no-deep-imports', rules['no-deep-imports'], {
  valid: [
    `import '@rookcom/monarch-tokens/css';`,
    `import { Button } from '@rookcom/monarch-react';`,
    `import x from './dist/thing.js';`,
    `const m = require('@rookcom/monarch-react/ai');`,
  ],
  invalid: [
    {
      code: `import '@rookcom/monarch-tokens/dist/css/monarch.css';`,
      output: `import '@rookcom/monarch-tokens/css';`,
      errors: [{ messageId: 'deep' }],
    },
    {
      code: `import { AiThread } from "@rookcom/monarch-react/dist/entries/ai.js";`,
      output: `import { AiThread } from "@rookcom/monarch-react/ai";`,
      errors: [{ messageId: 'deep' }],
    },
    {
      code: `export { Button } from '@rookcom/monarch-react/dist/index.js';`,
      output: `export { Button } from '@rookcom/monarch-react';`,
      errors: [{ messageId: 'deep' }],
    },
    {
      code: `export * from '@rookcom/monarch-react/dist/index.js';`,
      output: `export * from '@rookcom/monarch-react';`,
      errors: [{ messageId: 'deep' }],
    },
    {
      code: `const m = await import('@rookcom/monarch-react/dist/manifest.json');`,
      output: `const m = await import('@rookcom/monarch-react/manifest.json');`,
      errors: [{ messageId: 'deep' }],
    },
    {
      code: `const t = require('@rookcom/monarch-tokens/dist/tokens.json');`,
      output: `const t = require('@rookcom/monarch-tokens/tokens.json');`,
      errors: [{ messageId: 'deep' }],
    },
    {
      // No public path is known: reported, not rewritten.
      code: `import x from '@rookcom/monarch-react/dist/internal/x.js';`,
      output: null,
      errors: [{ messageId: 'deepUnknown' }],
    },
  ],
});

jsx.run('no-raw-colors', rules['no-raw-colors'], {
  valid: [
    `<div className="bg-mn-surface text-mn-fg-muted border-mn-line" />`,
    `<div className="bg-[hsl(var(--primary))]" />`,
    `<div style={{ color: 'var(--mn-color-text-default)' }} />`,
    // Outside className and style, a colour-like string is data.
    `const brand = '#ff0000';`,
    `<a href="#fff">x</a>`,
    `<div onClick={() => track('bg-red-500')} />`,
    { code: `tw('bg-red-500')`, options: [{ callees: ['cn'] }] },
  ],
  invalid: [
    {
      code: `<div className="p-4 bg-red-500 hover:text-slate-50/50" />`,
      errors: [
        { messageId: 'palette', data: { value: 'bg-red-500' }, column: 21 },
        { messageId: 'palette', data: { value: 'hover:text-slate-50/50' } },
      ],
    },
    { code: `<div class="text-white" />`, errors: [{ messageId: 'palette' }] },
    {
      code: `<div className={cn('a', on && 'bg-blue-600')} />`,
      errors: [{ messageId: 'palette' }],
    },
    { code: 'const c = clsx(`from-blue-600 ${x}`);', errors: [{ messageId: 'palette' }] },
    {
      code: `tw('bg-red-500')`,
      options: [{ callees: ['tw'] }],
      errors: [{ messageId: 'palette' }],
    },
    {
      code: `<div className="bg-[#123456]" />`,
      errors: [{ messageId: 'arbitrary', suggestions: [] }],
    },
    {
      code: `<div className="bg-[${themedHex}]" />`,
      errors: [
        {
          messageId: 'arbitrary',
          suggestions: [
            {
              messageId: 'useTokenReview',
              data: { token: themedToken.variable },
              output: `<div className="bg-[var(${themedToken.variable})]" />`,
            },
          ],
        },
      ],
    },
    {
      code: `<div style={{ color: '${themedHex}' }} />`,
      errors: [
        {
          messageId: 'literal',
          suggestions: [
            {
              messageId: 'useTokenReview',
              data: { token: themedToken.variable },
              output: `<div style={{ color: 'var(${themedToken.variable})' }} />`,
            },
          ],
        },
      ],
    },
    {
      code: `<div style={{ color: '#12345f', background: 'rgb(0 0 0 / 50%)', border: \`1px solid hsl(0 0% 0%)\` }} />`,
      errors: [
        { messageId: 'literal', suggestions: [] },
        { messageId: 'literal' },
        { messageId: 'literal' },
      ],
    },
  ],
});

jsx.run('no-arbitrary-spacing', rules['no-arbitrary-spacing'], {
  valid: [
    `<div className="p-4 gap-2 text-sm" />`,
    // On the 2px grid, as monarch check allows.
    `<div className="p-[18px] gap-[var(--mn-space-3)] mt-[calc(100%-2px)]" />`,
    `<div className="text-[var(--mn-font-size-sm)] text-[#fff]" />`,
    `const size = 'p-[7px]';`,
  ],
  invalid: [
    {
      code: `<div className="p-[7px] md:mx-[13px]" />`,
      errors: [
        { messageId: 'spacing', data: { value: 'p-[7px]' }, suggestions: [] },
        { messageId: 'spacing', data: { value: 'md:mx-[13px]' }, suggestions: [] },
      ],
    },
    {
      code: `<p className={cn('text-[13px] leading-[1.1]')} />`,
      errors: [{ messageId: 'type' }, { messageId: 'type' }],
    },
    {
      code: `<div className="p-[8px]" />`,
      options: [{ allowOnGrid: false }],
      errors: [
        {
          messageId: 'spacing',
          suggestions: [
            { messageId: 'useScale', data: { to: 'p-2' }, output: `<div className="p-2" />` },
          ],
        },
      ],
    },
    {
      code: `<div className="gap-[var(--x)] m-[18px]" />`,
      options: [{ allowOnGrid: false }],
      errors: [{ messageId: 'spacing', data: { value: 'm-[18px]' }, suggestions: [] }],
    },
  ],
});

describe('scaleClass', () => {
  it('maps a length on the scale to its class, and nothing else', () => {
    expect(scaleClass('p-[8px]')).toBe('p-2');
    expect(scaleClass('md:gap-[6px]')).toBe('md:gap-1.5');
    expect(scaleClass('mt-[1rem]')).toBe('mt-4');
    expect(scaleClass('p-[7px]')).toBeUndefined();
    expect(scaleClass('p-[8px_4px]')).toBeUndefined();
    expect(scaleClass('p-[2em]')).toBeUndefined();
  });
});

const aComponent = MONARCH_COMPONENTS.find((c) => c.name === 'Button') ? 'Button' : '';

jsx.run('prefer-monarch-component', rules['prefer-monarch-component'], {
  valid: [
    `function CheckoutSummaryForThisApp() { return null; }`,
    // A wrapper around the Monarch component is not a copy.
    `import { Button as Base } from '@rookcom/monarch-react';\nexport function Button(props) { return <Base {...props} />; }`,
    { code: `export function Button() { return null; }`, options: [{ allow: ['Button'] }] },
    // Not a component declaration.
    `const Button = 'primary';`,
    `function outer() { function Button() { return null; } return Button; }`,
  ],
  invalid: [
    {
      code: `export function ${aComponent}() { return <button />; }`,
      errors: [
        {
          messageId: 'duplicate',
          data: { name: 'Button', module: '@rookcom/monarch-react' },
        },
      ],
    },
    { code: `const Dialog = (props) => <div {...props} />;`, errors: [{ messageId: 'duplicate' }] },
    {
      code: `export const Card = forwardRef(function Inner(p, ref) { return <div ref={ref} />; });`,
      errors: [{ messageId: 'duplicate' }],
    },
    {
      code: `export default class Tabs extends Component {}`,
      errors: [{ messageId: 'duplicate' }],
    },
    {
      code: `import { Card } from '@rookcom/monarch-react';\nfunction Button() { return <Card />; }`,
      errors: [{ messageId: 'duplicate' }],
    },
  ],
});

describe('moduleOfComponent', () => {
  it('names the entry a component is imported from', () => {
    expect(moduleOfComponent('Button')).toBe('@rookcom/monarch-react');
    expect(moduleOfComponent('AiThread')).toBe('@rookcom/monarch-react/ai');
  });
});

const RENAMES = {
  deprecations: [
    { name: 'OldButton', package: '@rookcom/monarch-react', since: '0.2.0', replacement: 'Button' },
    { name: 'Gone', package: '@rookcom/monarch-react', since: '0.3.0', message: 'No replacement.' },
  ],
};

jsx.run('no-deprecated-api', rules['no-deprecated-api'], {
  valid: [
    `import { RookRail } from '@rookcom/monarch-react/rook';`,
    `import { AppShell } from '@rookcom/monarch-react/shell';`,
    `import { RookRail } from './shell';`,
    { code: `import { OldButton } from './local';`, options: [RENAMES] },
    {
      code: `import { RookRail } from '@rookcom/monarch-react/shell';`,
      options: [{ importPaths: [] }],
    },
  ],
  invalid: [
    {
      code: `import { RookRail, ROOK_RAIL_COOKIE } from '@rookcom/monarch-react/shell';`,
      output: `import { RookRail, ROOK_RAIL_COOKIE } from '@rookcom/monarch-react/rook';`,
      errors: [{ messageId: 'moved' }],
    },
    {
      code: `import {\n  AppShell,\n  RookRail as Rail,\n} from "@rookcom/monarch-react/shell";`,
      output: `import { AppShell } from "@rookcom/monarch-react/shell";\nimport { RookRail as Rail } from "@rookcom/monarch-react/rook";`,
      errors: [{ messageId: 'moved', line: 3 }],
    },
    {
      code: `import { SignInForm, SignInWithRookButton } from '@rookcom/monarch-react/patterns'`,
      output: `import { SignInForm } from '@rookcom/monarch-react/patterns'\nimport { SignInWithRookButton } from '@rookcom/monarch-react/rook'`,
      errors: [{ messageId: 'moved' }],
    },
    {
      code: `import { rookIntelligenceTransport } from '@rookcom/monarch-react/ai';`,
      output: `import { rookIntelligenceTransport } from '@rookcom/monarch-react/rook';`,
      errors: [{ messageId: 'moved' }],
    },
    {
      // A comment inside a mixed statement: reported, left for a person.
      code: `import { AppShell, /* keep */ RookRail } from '@rookcom/monarch-react/shell';`,
      output: null,
      errors: [{ messageId: 'moved' }],
    },
    {
      code: `import { OldButton } from '@rookcom/monarch-react';\n<OldButton />;`,
      output: `import { Button as OldButton } from '@rookcom/monarch-react';\n<OldButton />;`,
      options: [RENAMES],
      errors: [{ messageId: 'renamed' }],
    },
    {
      code: `import { OldButton as B } from '@rookcom/monarch-react';`,
      output: `import { Button as B } from '@rookcom/monarch-react';`,
      options: [RENAMES],
      errors: [{ messageId: 'renamed' }],
    },
    {
      // The replacement is imported already: no fix, the names would clash.
      code: `import { OldButton, Button } from '@rookcom/monarch-react';`,
      output: null,
      options: [RENAMES],
      errors: [{ messageId: 'renamed' }],
    },
    {
      code: `import { Gone } from '@rookcom/monarch-react/data';`,
      output: null,
      options: [RENAMES],
      errors: [{ messageId: 'deprecated' }],
    },
  ],
});

ts.run('no-deprecated-api (TypeScript)', rules['no-deprecated-api'], {
  valid: [`import type { AppShellProps } from '@rookcom/monarch-react/shell';`],
  invalid: [
    {
      code: `import type { RookRailProps } from '@rookcom/monarch-react/shell';`,
      output: `import type { RookRailProps } from '@rookcom/monarch-react/rook';`,
      errors: [{ messageId: 'moved' }],
    },
    {
      code: `import { AppShell, RookRail, type RookRailProps } from '@rookcom/monarch-react/shell';`,
      output: `import { AppShell } from '@rookcom/monarch-react/shell';\nimport { RookRail, type RookRailProps } from '@rookcom/monarch-react/rook';`,
      errors: [{ messageId: 'moved' }],
    },
  ],
});

jsx.run('no-rook-specific', rules['no-rook-specific'], {
  valid: [
    `import { AppShell, ThemeToggle } from '@rookcom/monarch-react/shell';`,
    `import '@rookcom/monarch-tokens/css/products/neutral';`,
    `import { RookRail } from './my-rail';`,
  ],
  invalid: [
    {
      code: `import { RookRail, AppShell, ROOK_THEME_COOKIE } from '@rookcom/monarch-react/shell';`,
      errors: [
        {
          messageId: 'rookExport',
          data: { name: 'RookRail', alternative: alternativeFor('RookRail') },
        },
        {
          messageId: 'rookExport',
          data: { name: 'ROOK_THEME_COOKIE', alternative: alternativeFor('ROOK_THEME_COOKIE') },
        },
      ],
    },
    {
      code: `import { SignInWithRookButton } from '@rookcom/monarch-react/rook';`,
      errors: [{ messageId: 'rookExport' }],
    },
    {
      code: `import * as rook from '@rookcom/monarch-react/rook';`,
      errors: [{ messageId: 'rookEntry' }],
    },
    {
      code: `import '@rookcom/monarch-tokens/css/products/tickets';`,
      errors: [{ messageId: 'productAccent', data: { product: 'tickets' } }],
    },
  ],
});

describe('alternativeFor', () => {
  it('names the general component for each Rook export', () => {
    expect(alternativeFor('RookRail')).toContain('SidebarNav');
    expect(alternativeFor('RookBarAppCatalog')).toContain('SiteHeader');
    expect(alternativeFor('SignInWithRookButton')).toContain('SignInForm');
    expect(alternativeFor('rookIntelligenceTransport')).toContain('createSseTransport');
    expect(alternativeFor('ROOK_THEME_EVENT')).toContain('ThemeToggle');
    expect(alternativeFor('RookSomethingNew')).toContain('general component');
  });
});

jsx.run('require-accessible-name', rules['require-accessible-name'], {
  valid: [
    `import { IconButton } from '@rookcom/monarch-react';\n<IconButton icon={<X />} label="Close" />;`,
    `import { IconButton } from '@rookcom/monarch-react';\n<IconButton icon={<X />} aria-labelledby="t" />;`,
    `import { IconButton } from '@rookcom/monarch-react';\n<IconButton {...props} />;`,
    `import { Button } from '@rookcom/monarch-react';\n<Button>Save</Button>;`,
    `import { Button } from '@rookcom/monarch-react';\n<Button leadingIcon={<Plus />}>{label}</Button>;`,
    `import { Button } from '@rookcom/monarch-react';\n<Button aria-label="Delete"><TrashIcon /></Button>;`,
    `import { Button } from '@rookcom/monarch-react';\n<Button asChild><a href="/x">Go</a></Button>;`,
    `import { Button } from '@rookcom/monarch-react';\n<Button><Trans id="save" /></Button>;`,
    // Not Monarch's.
    `import { IconButton } from './ui';\n<IconButton />;`,
  ],
  invalid: [
    {
      code: `import { IconButton } from '@rookcom/monarch-react';\n<IconButton icon={<X />} />;`,
      errors: [{ messageId: 'iconButton', line: 2 }],
    },
    {
      code: `import { IconButton as IB } from '@rookcom/monarch-react';\n<IB icon={<X />} label="" />;`,
      errors: [{ messageId: 'emptyLabel', data: { prop: 'label' } }],
    },
    {
      code: `import { Button } from '@rookcom/monarch-react';\n<Button leadingIcon={<Trash />} />;`,
      errors: [{ messageId: 'button' }],
    },
    {
      code: `import { Button } from '@rookcom/monarch-react';\n<Button>\n  <TrashIcon />\n  {/* delete */}\n</Button>;`,
      errors: [{ messageId: 'button' }],
    },
    {
      code: `import { Button } from '@rookcom/monarch-react';\n<Button aria-label={''}><svg /></Button>;`,
      errors: [{ messageId: 'emptyLabel', data: { prop: 'aria-label' } }],
    },
  ],
});

const ENTRY_OPTIONS = [{ styles: ['@rookcom/monarch-react/styles/core.css'] }];

jsx.run('require-entry-styles', rules['require-entry-styles'], {
  valid: [
    // Unconfigured: the rule cannot know what the app loads.
    `import { AiThread } from '@rookcom/monarch-react/ai';`,
    { code: `import { Button } from '@rookcom/monarch-react';`, options: ENTRY_OPTIONS },
    {
      code: `import { AiThread } from '@rookcom/monarch-react/ai';`,
      options: [{ styles: ['@rookcom/monarch-react/styles.css'] }],
    },
    {
      code: `import { AiThread } from '@rookcom/monarch-react/ai';`,
      options: [
        {
          styles: [
            '@rookcom/monarch-react/styles/core.css',
            '@rookcom/monarch-react/styles/ai.css',
          ],
        },
      ],
    },
    {
      code: `import { AiThread } from '@rookcom/monarch-react/ai';`,
      options: [{ styles: ['@rookcom/monarch-react/styles/components/ai-thread.css'] }],
    },
    // Hooks and helpers render nothing.
    {
      code: `import { useAiConversation, mockTransport } from '@rookcom/monarch-react/ai';`,
      options: ENTRY_OPTIONS,
    },
    // The sheet of the entry that owns a Rook export styles it too.
    {
      code: `import { RookRail } from '@rookcom/monarch-react/rook';`,
      options: [{ styles: ['@rookcom/monarch-react/styles/shell.css'] }],
    },
  ],
  invalid: [
    {
      code: `import { Button } from '@rookcom/monarch-react';\nimport { AiThread } from '@rookcom/monarch-react/ai';`,
      options: ENTRY_OPTIONS,
      errors: [{ messageId: 'missing', line: 2 }],
    },
    {
      code: `import { RookRail } from '@rookcom/monarch-react/rook';`,
      options: ENTRY_OPTIONS,
      errors: [{ messageId: 'missing' }],
    },
    {
      code: `import * as ai from '@rookcom/monarch-react/ai';`,
      options: [{ styles: [] }],
      errors: [{ messageId: 'missing' }],
    },
  ],
});

ts.run('require-entry-styles (TypeScript)', rules['require-entry-styles'], {
  valid: [
    { code: `import type { AiMessage } from '@rookcom/monarch-react/ai';`, options: ENTRY_OPTIONS },
    { code: `import { type AiMessage } from '@rookcom/monarch-react/ai';`, options: ENTRY_OPTIONS },
  ],
  invalid: [
    {
      code: `import { AiThread, type AiMessage } from '@rookcom/monarch-react/ai';`,
      options: ENTRY_OPTIONS,
      errors: [{ messageId: 'missing' }],
    },
  ],
});

describe('require-entry-styles root files', () => {
  const dir = mkdtempSync(join(tmpdir(), 'monarch-eslint-'));
  afterAll(() => rmSync(dir, { recursive: true, force: true }));

  it('reads the stylesheets the configured root files import, and nothing else', () => {
    writeFileSync(
      join(dir, 'layout.tsx'),
      `import '@rookcom/monarch-react/styles/core.css';\nimport '@rookcom/monarch-react/styles/ai.css';\n`,
    );
    writeFileSync(
      join(dir, 'globals.css'),
      `@import '@rookcom/monarch-react/styles/components/kanban.css';\n`,
    );
    const styles = configuredStyles(
      dir,
      ['@rookcom/monarch-react/styles/data.css'],
      ['layout.tsx', 'globals.css', 'missing.tsx'],
    );
    expect(styles.all).toBe(false);
    expect([...styles.entries].sort()).toEqual(['ai', 'core', 'data']);
    expect([...styles.components]).toEqual(['kanban']);
  });

  it('sees a root file change on the next run', () => {
    const file = join(dir, 'app.css');
    writeFileSync(file, `@import '@rookcom/monarch-react/styles/core.css';`);
    expect(configuredStyles(dir, [], ['app.css']).all).toBe(false);
    writeFileSync(file, `@import '@rookcom/monarch-react/styles.css'; /* everything */`);
    // A fresh mtime: some file systems keep millisecond stamps.
    const linter = new Linter({ configType: 'flat' });
    const run = () =>
      linter.verify(
        `import { AiThread } from '@rookcom/monarch-react/ai';`,
        [
          {
            plugins: { monarch: plugin },
            rules: { 'monarch/require-entry-styles': ['warn', { rootFiles: [file] }] },
          },
        ],
        { filename: 'a.js' },
      );
    expect(configuredStyles(dir, [], ['app.css']).all || run().length === 0).toBe(true);
  });
});
