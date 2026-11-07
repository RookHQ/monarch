import { describe, expect, it } from 'vitest';
import { readSuppressions, readTags, type RawFinding } from '../src/rules.js';
import { rules, scan } from './helpers.js';

const only = (rule: string, fs: readonly RawFinding[]) => fs.filter((f) => f.rule === rule);

describe('color/no-raw-palette', () => {
  it('flags palette classes with variants and opacity', () => {
    const f = only(
      'color/no-raw-palette',
      scan(
        'a.tsx',
        `<p className="text-red-500 hover:bg-slate-50/50 dark:from-blue-600 md:hover:border-x-zinc-200 bg-white/10" />`,
      ),
    );
    expect(f.map((x) => x.message)).toEqual([
      'Palette class `text-red-500`',
      'Palette class `hover:bg-slate-50/50`',
      'Palette class `dark:from-blue-600`',
      'Palette class `md:hover:border-x-zinc-200`',
      'Palette class `bg-white/10`',
    ]);
  });

  it('flags arbitrary colour classes but not token-backed ones', () => {
    const f = only(
      'color/no-raw-palette',
      scan(
        'a.tsx',
        `"bg-[#fff] text-[rgb(1,2,3)] bg-[hsl(var(--primary))] bg-mn-surface text-muted-foreground"`,
      ),
    );
    expect(f.map((x) => x.message)).toEqual([
      'Arbitrary colour class `bg-[#fff]`',
      'Arbitrary colour class `text-[rgb(1,2,3)]`',
    ]);
  });

  it('ignores semantic names and lookalike words', () => {
    expect(
      only(
        'color/no-raw-palette',
        scan(
          'a.tsx',
          `"bg-primary text-destructive border-border ring-ring red-500 my-text-red-500x"`,
        ),
      ),
    ).toHaveLength(0);
  });

  it('checks @apply in CSS', () => {
    expect(
      only(
        'color/no-raw-palette',
        scan('a.css', `.x { @apply bg-gray-100; }\n.y { background: red; }`),
      ),
    ).toHaveLength(1);
  });
});

describe('color/no-hard-coded-color', () => {
  it('flags hex and colour functions', () => {
    const f = only(
      'color/no-hard-coded-color',
      scan(
        'a.ts',
        `const a = '#fff'; const b = "rgba(0, 0, 0, 0.5)"; const c = 'hsl(220 10% 50%)';`,
      ),
    );
    expect(f).toHaveLength(3);
  });

  it('does not flag anchors, SVG references, issue numbers or token functions', () => {
    const text = [
      `<a href="#add">x</a>`,
      `<Link to="#face" />`,
      `<path fill="url(#a1b2c3)" />`,
      `const msg = 'Ticket #1234 created';`,
      `const order = 'Order #1042';`,
      `const v = 'hsl(var(--primary))';`,
      `// const c = '#ffffff';`,
    ].join('\n');
    expect(only('color/no-hard-coded-color', scan('a.tsx', text))).toEqual([]);
  });

  it('skips design-token definitions in CSS but checks app-local variables', () => {
    const f = only(
      'color/no-hard-coded-color',
      scan('a.css', `:root {\n  --primary: #dc2e3e;\n  --rs-bg: #15181f;\n}`),
    );
    expect(f).toHaveLength(1);
    expect(f[0]?.line).toBe(3);
  });
});

describe('typography and spacing', () => {
  it('flags arbitrary type values but allows variables', () => {
    const f = only(
      'typography/no-arbitrary-type',
      scan(
        'a.tsx',
        `"text-[13px] font-[550] leading-[1.1] tracking-[0.3em] text-[var(--mn-text-sm)] text-[#fff]"`,
      ),
    );
    expect(f.map((x) => x.message)).toEqual([
      'Arbitrary type value `text-[13px]`',
      'Arbitrary type value `font-[550]`',
      'Arbitrary type value `leading-[1.1]`',
      'Arbitrary type value `tracking-[0.3em]`',
    ]);
  });

  it('flags local font-family in CSS and inline styles', () => {
    expect(
      only(
        'typography/no-arbitrary-type',
        scan('a.css', `.x { font-family: Arial; }\n.y { font-family: var(--mn-font-sans); }`),
      ),
    ).toHaveLength(1);
    expect(
      only(
        'typography/no-arbitrary-type',
        scan('a.tsx', `<p style={{ fontFamily: 'Georgia' }} />`),
      ),
    ).toHaveLength(1);
  });

  it('allows the 2px half-grid, hairlines and relative units', () => {
    const f = only(
      'spacing/off-grid',
      scan(
        'a.tsx',
        `"p-[7px] gap-[3px] -mt-[13px] p-[6px] m-[1px] gap-[0.5rem] p-[var(--mn-space-2)] px-[5%] p-[calc(100%-8px)]"`,
      ),
    );
    expect(f.map((x) => x.message)).toEqual([
      'Off-grid spacing `p-[7px]`',
      'Off-grid spacing `gap-[3px]`',
      'Off-grid spacing `-mt-[13px]`',
    ]);
  });

  it('checks CSS padding, margin and gap but not em', () => {
    const f = only(
      'spacing/off-grid',
      scan('a.css', `.a { padding: 7px 8px; }\n.b { margin: 1.4em; }\n.c { gap: 12px; }`),
    );
    expect(f).toHaveLength(1);
  });
});

describe('tokens, overrides and legacy variables', () => {
  it('groups local token definitions per block', () => {
    const f = only(
      'tokens/local-token-definitions',
      scan(
        'g.css',
        `:root {\n  --background: 0 0% 99%;\n  --primary: 1 2% 3%;\n  --sidebar-ring: 1 2% 3%;\n  --app: 1px;\n}\n.dark {\n  --background: 0 0% 9%;\n}`,
      ),
    );
    expect(f).toHaveLength(2);
    expect(f[0]?.message).toContain('3 design-token variables defined in `:root`');
    expect(f[1]?.message).toContain('1 design-token variable defined in `.dark`');
  });

  it('flags Monarch internals in CSS and code', () => {
    const css = only(
      'overrides/monarch-internals',
      scan('a.css', `.mn-button { color: red; }\n:root {\n  --mn-color-bg-canvas: white;\n}`),
    );
    expect(css).toHaveLength(2);
    const code = only(
      'overrides/monarch-internals',
      scan(
        'a.tsx',
        `<div className="[&_.mn-button]:p-2" style={{ '--mn-radius-lg': '0' } as never} />`,
      ),
    );
    expect(code).toHaveLength(2);
    expect(
      only(
        'overrides/monarch-internals',
        scan('a.css', `.x { color: var(--mn-color-text-default); }`),
      ),
    ).toHaveLength(0);
  });

  it('tracks legacy bridge variables as info', () => {
    const f = only(
      'legacy/bridge-variable',
      scan(
        'a.css',
        `.x { color: hsl(var(--primary)); background: var(--mn-color-bg-canvas); border-color: hsl(var(--sidebar-border)); }`,
      ),
    );
    expect(f).toHaveLength(2);
  });
});

describe('components/duplicated-primitive', () => {
  it('matches Monarch stems in ui folders only', () => {
    expect(rules(scan('src/components/ui/button.tsx', ''))).toContain(
      'components/duplicated-primitive',
    );
    expect(rules(scan('src/components/ui/DropdownMenu.tsx', ''))).toContain(
      'components/duplicated-primitive',
    );
    expect(rules(scan('src/components/ui/empty-state/index.tsx', ''))).toContain(
      'components/duplicated-primitive',
    );
    expect(rules(scan('src/components/ui/picker.tsx', ''))).not.toContain(
      'components/duplicated-primitive',
    );
    expect(rules(scan('src/app/button.tsx', ''))).not.toContain('components/duplicated-primitive');
  });
});

describe('accessibility', () => {
  it('img-alt: requires alt unless props are spread', () => {
    const f = only(
      'a11y/img-alt',
      scan(
        'a.tsx',
        `<img src="a.png" />\n<img src="b.png" alt="" />\n<img {...props} />\n<img\n  src={x}\n  onLoad={() => a > b}\n/>`,
      ),
    );
    expect(f.map((x) => x.line)).toEqual([1, 4]);
  });

  it('click-without-key: flags non-interactive click targets', () => {
    const text = [
      `<div onClick={() => go()}>a</div>`,
      `<div role="button" tabIndex={0} onClick={go} onKeyDown={key}>b</div>`,
      `<div role="button" onClick={go}>c</div>`,
      `<div aria-hidden onClick={close} />`,
      `<div onClick={(e) => e.stopPropagation()}>d</div>`,
      `<button onClick={go}>e</button>`,
      `<div {...rest} onClick={go} />`,
    ].join('\n');
    const f = only('a11y/click-without-key', scan('a.tsx', text));
    expect(f.map((x) => [x.line, x.message])).toEqual([
      [1, '<div onClick> without role and onKeyDown'],
      [3, '<div onClick> without onKeyDown'],
    ]);
  });

  it('positive-tabindex', () => {
    expect(
      only(
        'a11y/positive-tabindex',
        scan('a.tsx', `<a tabIndex={3} /><a tabIndex={0} /><a tabIndex="-1" /><a tabIndex="2" />`),
      ),
    ).toHaveLength(2);
  });

  it('outline-none needs a nearby focus replacement', () => {
    expect(only('a11y/outline-none-without-ring', scan('a.tsx', `"outline-none"`))).toHaveLength(1);
    expect(
      only('a11y/outline-none-without-ring', scan('a.tsx', `"focus:outline-none focus:ring-0"`)),
    ).toHaveLength(1);
    expect(
      only(
        'a11y/outline-none-without-ring',
        scan('a.tsx', `"outline-none focus-visible:ring-2 focus-visible:ring-ring"`),
      ),
    ).toHaveLength(0);
    expect(
      only(
        'a11y/outline-none-without-ring',
        scan('a.tsx', `cva(\n  'outline-none',\n  'focus-visible:ring-2',\n)`),
      ),
    ).toHaveLength(0);
    expect(
      only(
        'a11y/outline-none-without-ring',
        scan('a.tsx', `"outline-none data-[highlighted]:bg-muted"`),
      ),
    ).toHaveLength(0);
  });

  it('click-without-key: allows composite items in the aria-activedescendant pattern', () => {
    const listbox = [
      `<input role="combobox" aria-activedescendant={activeId} onKeyDown={onKey} />`,
      `<ul role="listbox">`,
      `  <li role="option" id="opt-1" onClick={() => pick(1)}>One</li>`,
      `  <div role="menuitem" onClick={go}>Go</div>`,
      `</ul>`,
    ].join('\n');
    expect(only('a11y/click-without-key', scan('a.tsx', listbox))).toEqual([]);
    // Without the pattern in the file, the same items are still flagged.
    const plain = `<li role="option" onClick={() => pick(1)}>One</li>`;
    expect(only('a11y/click-without-key', scan('b.tsx', plain))).toHaveLength(1);
    // A plain div is flagged even when the file uses the pattern.
    const mixed = `<input aria-activedescendant={a} />\n<div onClick={go}>x</div>`;
    expect(only('a11y/click-without-key', scan('c.tsx', mixed))).toHaveLength(1);
  });

  it('outline-none: accepts a focus-within ring on a wrapper above', () => {
    const group = [
      `<div className="flex rounded-md border focus-within:ring-2 focus-within:ring-ring">`,
      `  <Icon />`,
      `  <span>prefix</span>`,
      `  <input`,
      `    value={v}`,
      `    onChange={set}`,
      `    className="flex-1 bg-transparent outline-none"`,
      `  />`,
      `</div>`,
    ].join('\n');
    expect(only('a11y/outline-none-without-ring', scan('a.tsx', group))).toEqual([]);
    // focus-within:outline-none is not a replacement.
    const bad = `<div className="focus-within:outline-none">\n  <input className="outline-none" />\n</div>`;
    expect(only('a11y/outline-none-without-ring', scan('b.tsx', bad))).toHaveLength(2);
  });

  it('reads JSX tags across lines with nested braces', () => {
    const tags = readTags(
      `<div\n  onClick={() => { if (a > b) go(); }}\n  className="x"\n>`,
      /div/,
    );
    expect(tags).toHaveLength(1);
    expect(tags[0]?.text.endsWith('>')).toBe(true);
    expect(tags[0]?.text).toContain('className');
  });
});

describe('api/deprecated', () => {
  const dep = [
    { name: 'OldButton', package: '@rookcom/monarch-react', since: '0.2.0', replacement: 'Button' },
  ];
  it('reports deprecated names only in files importing the package', () => {
    const f = only(
      'api/deprecated',
      scan('a.tsx', `import { OldButton } from '@rookcom/monarch-react';\n<OldButton />`, dep),
    );
    expect(f).toHaveLength(2);
    expect(f[0]?.message).toContain('use `Button`');
    expect(
      only(
        'api/deprecated',
        scan('b.tsx', `import { OldButton } from './local';\n<OldButton />`, dep),
      ),
    ).toHaveLength(0);
  });
});

describe('suppressions', () => {
  it('parses rules and reasons in line and block comments', () => {
    const s = readSuppressions([
      '// monarch-ignore-next-line color/no-raw-palette -- partner brand',
      'x',
      '{/* monarch-ignore-next-line a11y/img-alt, color/* -- decorative */}',
      'y',
      '/* monarch-ignore-next-line spacing/off-grid */',
    ]);
    expect(s.map((x) => [x.target, x.rules, x.reason])).toEqual([
      [2, ['color/no-raw-palette'], 'partner brand'],
      [4, ['a11y/img-alt', 'color/*'], 'decorative'],
      [6, ['spacing/off-grid'], null],
    ]);
  });

  it('skips comment-only lines (e.g. an eslint directive) to reach the code', () => {
    const s = readSuppressions([
      '// monarch-ignore-next-line a11y/click-without-key -- activedescendant pattern',
      '// eslint-disable-next-line jsx-a11y/click-events-have-key-events',
      '{/* another note */}',
      '<div onClick={pick} />',
    ]);
    expect(s[0]?.target).toBe(4);
  });
});

describe('minified CSS lines', () => {
  it('checks every declaration on a line', () => {
    const f = scan(
      'a.css',
      `.a{padding:7px}.b{margin:3px}.c{font-family:Arial}.d{font-family:Georgia}`,
    );
    expect(f.filter((x) => x.rule === 'spacing/off-grid')).toHaveLength(2);
    expect(f.filter((x) => x.rule === 'typography/no-arbitrary-type')).toHaveLength(2);
  });
});

describe('icons/no-sparkles', () => {
  const ids = (f: RawFinding[]) =>
    f.filter((x) => x.rule === 'icons/no-sparkles').map((x) => x.line);
  it('flags sparkle icons imported from icon libraries, including aliases and Wand2', () => {
    const f = scan(
      'a.tsx',
      [
        "import { Bot, Sparkles, Wand2 as Magic } from 'lucide-react';",
        '<Sparkles />',
        '<Magic className="x" />',
        '<Bot />',
      ].join('\n'),
    );
    expect(ids(f)).toEqual([1, 1, 2, 3]);
  });
  it('ignores local components that merely share the name', () => {
    expect(ids(scan('b.tsx', "import { Sparkles } from './my-sparkles';\n<Sparkles />"))).toEqual(
      [],
    );
  });
});

describe('dialogs/no-native-dialog', () => {
  const lines = (f: readonly RawFinding[]) =>
    only('dialogs/no-native-dialog', f).map((x) => [x.line, x.column]);

  it('flags window, globalThis and bare calls to confirm, alert and prompt', () => {
    const f = scan(
      'a.tsx',
      [
        "if (!window.confirm('Delete tile?')) return;",
        "const name = prompt('Name this view');",
        "alert('Download failed');",
        "globalThis.confirm('Sure?');",
        '// window.confirm("commented out")',
      ].join('\n'),
    );
    expect(lines(f)).toEqual([
      [1, 6],
      [2, 14],
      [3, 1],
      [4, 1],
    ]);
    expect(only('dialogs/no-native-dialog', f)[1]?.message).toContain('usePrompt()');
  });

  it('allows the app’s own confirm, such as Monarch’s useConfirm', () => {
    const f = scan(
      'b.tsx',
      [
        'const { confirm, element } = useConfirm();',
        "if (await confirm({ title: 'Delete note?' })) remove();",
        'const { prompt: ask } = usePrompt();',
        'function alert(x: string) { log(x); }',
        "alert('kept local');",
      ].join('\n'),
    );
    expect(lines(f)).toEqual([]);
  });

  it('still flags window.confirm when a local confirm exists, and ignores members and methods', () => {
    const f = scan(
      'c.ts',
      [
        'const { confirm } = useConfirm();',
        "window.confirm('old');",
        "dialog.confirm('x'); api.alert(1);",
        'class A { async prompt(input: string) { return input; } }',
      ].join('\n'),
    );
    expect(lines(f)).toEqual([[2, 1]]);
  });

  it('ignores the words inside strings', () => {
    const f = scan(
      'e.ts',
      [
        "const label = 'New booking alert (staff)';",
        'const t = `Confirm (optional)`; const u = "prompt (text)";',
        "notify(`x`); window.alert('real');",
      ].join('\n'),
    );
    expect(lines(f)).toEqual([[3, 14]]);
  });

  it('does not scan CSS', () => {
    expect(lines(scan('d.css', '.x { content: "confirm("; }'))).toEqual([]);
  });
});
