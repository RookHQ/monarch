import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';
import {
  FigmaConnectError,
  codeConnectSource,
  figmaUrlProblem,
  generateFigmaConnect,
  parseFigmaMap,
  titleCase,
  type FigmaMapEntry,
} from '../src/figma-connect.js';
import type { Manifest, ManifestExport, ManifestProp } from '../src/manifest.js';
import { here, tempDir, write } from './helpers.js';

const URL_OK = 'https://www.figma.com/design/AbC123xyz/Product-UI?node-id=12-345&t=abc';
const PLACEHOLDER_URL = 'https://www.figma.com/design/<FILE_KEY>/<FILE_NAME>?node-id=<NODE_ID>';

const prop = (name: string, type: string, extra: Partial<ManifestProp> = {}): ManifestProp => ({
  name,
  type,
  required: false,
  description: '',
  ...extra,
});

const component = (
  name: string,
  props: readonly ManifestProp[],
  extra: Partial<ManifestExport> = {},
): ManifestExport => ({
  name,
  entry: '.',
  kind: 'component',
  description: '',
  clientOnly: false,
  props,
  ...extra,
});

const BUTTON = component('Button', [
  prop('variant', 'ButtonVariant', { values: ['secondary', 'primary', 'danger-subtle'] }),
  prop('trailingIcon', 'ReactNode'),
  prop('onClick', '(() => void)'),
  prop('label', 'string'),
  prop('disabled', 'boolean'),
  prop('className', 'string'),
  prop('style', 'CSSProperties'),
  prop('id', 'string'),
  prop('aria-label', 'string'),
  prop('ref', 'Ref<HTMLButtonElement>'),
  prop('columns', '2 | 3', { values: [2, 3] }),
  prop('span', 'WidgetSpan', { values: [1, 'full'] }),
  prop('options', 'readonly string[]'),
  prop('count', 'number'),
  prop('children', 'ReactNode'),
]);

const MANIFEST: Manifest = {
  package: '@rookcom/monarch-react',
  version: '0.0.0',
  styles: '',
  tokens: '',
  components: [
    BUTTON,
    component('AiComposer', [prop('placeholder', 'string | undefined')], { entry: './ai' }),
    component('Divider', []),
    { ...component('useToast', []), kind: 'hook' },
    { ...component('formatDate', []), kind: 'utility' },
  ],
};

const entry = (name: string, extra: Partial<FigmaMapEntry> = {}): FigmaMapEntry => ({
  component: name,
  url: URL_OK,
  ...extra,
});

describe('parseFigmaMap', () => {
  it('accepts a valid map and keeps overrides', () => {
    const map = parseFigmaMap({
      components: [
        {
          component: 'Button',
          url: URL_OK,
          props: { variant: 'Type' },
          values: { variant: { primary: 'Main' } },
        },
        { component: 'Badge', url: 'https://figma.com/file/AbC/Old?node-id=1%3A2' },
      ],
    });
    expect(map.components).toHaveLength(2);
    expect(map.components[0]?.props).toEqual({ variant: 'Type' });
    expect(map.components[0]?.values).toEqual({ variant: { primary: 'Main' } });
    expect(map.components[1]?.props).toBeUndefined();
  });

  it.each([
    [null],
    [[]],
    [{}],
    [{ components: 'Button' }],
    [{ components: ['Button'] }],
    [{ components: [{ url: URL_OK }] }],
    [{ components: [{ component: '', url: URL_OK }] }],
    [{ components: [{ component: 'Button' }] }],
    [{ components: [{ component: 'Button', url: URL_OK, props: ['Variant'] }] }],
    [{ components: [{ component: 'Button', url: URL_OK, props: { variant: 3 } }] }],
    [{ components: [{ component: 'Button', url: URL_OK, props: { variant: '' } }] }],
    [{ components: [{ component: 'Button', url: URL_OK, values: { variant: 'Main' } }] }],
    [{ components: [{ component: 'Button', url: URL_OK, values: { variant: { primary: 1 } } }] }],
  ])('rejects a wrong shape: %j', (value) => {
    expect(() => parseFigmaMap(value)).toThrow(FigmaConnectError);
  });

  it('rejects a component listed twice', () => {
    expect(() => parseFigmaMap({ components: [entry('Button'), entry('Button')] })).toThrow(
      /"Button" is listed more than once/,
    );
  });

  it.each([
    ['http://www.figma.com/design/AbC/File?node-id=1-2', /must start with https/],
    ['https://figma.com.evil.example/design/AbC/File?node-id=1-2', /must start with https/],
    ['https://www.figma.com/proto/AbC/File?node-id=1-2', /design or file URL/],
    ['https://www.figma.com/design/AbC/File', /no node-id/],
    ['https://www.figma.com/design/AbC/File?node-id=', /no node-id/],
    ['https://www.figma.com/design/AbC/File?node-id=abc', /not a Figma node id/],
    ['not a url', /is not a URL/],
  ])('rejects the URL %s', (url, message) => {
    expect(() => parseFigmaMap({ components: [entry('Button', { url })] })).toThrow(message);
  });

  it('accepts the node id forms Figma uses', () => {
    for (const id of ['1-2', '1:2', 'I5666:18;1:105']) {
      expect(
        figmaUrlProblem(`https://www.figma.com/design/AbC/F?node-id=${encodeURIComponent(id)}`),
      ).toBeUndefined();
    }
  });

  it('rejects placeholder URLs and says what to do', () => {
    expect(() =>
      parseFigmaMap({ components: [entry('Button', { url: PLACEHOLDER_URL })] }),
    ).toThrow(/components\[0\]\.url \(Button\) is a placeholder.*Copy link to selection/);
    expect(figmaUrlProblem('https://www.figma.com/design/YOUR_FILE/Name?node-id=1-2')).toMatch(
      /placeholder/,
    );
    expect(figmaUrlProblem('https://www.figma.com/design/%3CFILE%3E/Name?node-id=1-2')).toMatch(
      /placeholder/,
    );
  });

  it('rejects the shipped figma/map.example.json', () => {
    const example = join(here, '../../../figma/map.example.json');
    const json: unknown = JSON.parse(readFileSync(example, 'utf8'));
    expect(() => parseFigmaMap(json)).toThrow(/placeholder/);
  });
});

describe('titleCase', () => {
  it('splits camelCase, hyphens and underscores', () => {
    expect(titleCase('leadingIcon')).toBe('Leading Icon');
    expect(titleCase('danger-subtle')).toBe('Danger Subtle');
    expect(titleCase('snake_case')).toBe('Snake Case');
    expect(titleCase('xs')).toBe('Xs');
  });
});

describe('codeConnectSource', () => {
  const source = codeConnectSource(BUTTON, entry('Button'));

  it('imports figma and the component from its module', () => {
    expect(source).toContain(`import figma from '@figma/code-connect';`);
    expect(source).toContain(`import { Button } from '@rookcom/monarch-react';`);
    const ai = MANIFEST.components[1] as ManifestExport;
    expect(codeConnectSource(ai, entry('AiComposer'))).toContain(
      `import { AiComposer } from '@rookcom/monarch-react/ai';`,
    );
  });

  it('connects the component to the URL', () => {
    expect(source).toContain(`figma.connect(Button, '${URL_OK}', {`);
  });

  it('maps string values to figma.enum with Title Case labels, sorted', () => {
    expect(source).toContain(
      `    variant: figma.enum('Variant', { 'Danger Subtle': 'danger-subtle', Primary: 'primary', Secondary: 'secondary' }),`,
    );
  });

  it('maps boolean, string and children', () => {
    expect(source).toContain(`    disabled: figma.boolean('Disabled'),`);
    expect(source).toContain(`    label: figma.string('Label'),`);
    expect(source).toContain(`    children: figma.children('*'),`);
  });

  it('treats "T | undefined" as T', () => {
    const ai = MANIFEST.components[1] as ManifestExport;
    expect(codeConnectSource(ai, entry('AiComposer'))).toContain(
      `placeholder: figma.string('Placeholder'),`,
    );
  });

  it('maps string children to figma.string', () => {
    const tag = component('Tag', [prop('children', 'string')]);
    expect(codeConnectSource(tag, entry('Tag'))).toContain(`children: figma.string('Children'),`);
  });

  it('leaves out functions, objects, refs, numbers, other nodes and HTML plumbing', () => {
    for (const name of [
      'trailingIcon',
      'onClick',
      'className',
      'style',
      'id:',
      'aria-label',
      'ref',
      'columns',
      'span',
      'options',
      'count',
    ]) {
      expect(source).not.toContain(`    ${name}`);
    }
  });

  it('sorts props by name with children last, and uses every one in the example', () => {
    const names = [...source.matchAll(/^ {4}(\w+): figma\./gm)].map((m) => m[1]);
    expect(names).toEqual(['disabled', 'label', 'variant', 'children']);
    expect(source).toContain(
      '  example: ({ disabled, label, variant, children }) => <Button disabled={disabled} label={label} variant={variant}>{children}</Button>,',
    );
  });

  it('writes a self-closing example without children and no props block without props', () => {
    const ai = MANIFEST.components[1] as ManifestExport;
    expect(codeConnectSource(ai, entry('AiComposer'))).toContain(
      '  example: ({ placeholder }) => <AiComposer placeholder={placeholder} />,',
    );
    const divider = codeConnectSource(component('Divider', []), entry('Divider'));
    expect(divider).not.toContain('props:');
    expect(divider).toContain('  example: () => <Divider />,');
  });

  it('applies Figma property name and option label overrides', () => {
    const overridden = codeConnectSource(
      BUTTON,
      entry('Button', {
        props: { variant: 'Type', disabled: 'Is Disabled', label: 'Text' },
        values: { variant: { 'danger-subtle': 'Danger (subtle)' } },
      }),
    );
    expect(overridden).toContain(
      `variant: figma.enum('Type', { 'Danger (subtle)': 'danger-subtle', Primary: 'primary', Secondary: 'secondary' }),`,
    );
    expect(overridden).toContain(`disabled: figma.boolean('Is Disabled'),`);
    expect(overridden).toContain(`label: figma.string('Text'),`);
  });

  it('escapes quotes, backslashes and line breaks in strings', () => {
    const odd = component('Odd', [prop('tone', 'Tone', { values: ["it's", 'a\\b'] })]);
    const text = codeConnectSource(
      odd,
      entry('Odd', { props: { tone: "Designer's\ntone" }, values: { tone: { "it's": "It's" } } }),
    );
    expect(text).toContain(
      `tone: figma.enum('Designer\\'s\\ntone', { 'A\\\\b': 'a\\\\b', 'It\\'s': 'it\\'s' }),`,
    );
  });

  it('escapes line and paragraph separators', () => {
    const text = codeConnectSource(
      component('Sep', [prop('label', 'string')]),
      entry('Sep', { props: { label: 'A\u2028B\u2029C' } }),
    );
    expect(text).toContain("label: figma.string('A\\u2028B\\u2029C'),");
    expect(text).not.toMatch(/[\u2028\u2029]/);
  });

  it('never writes two options with the same label', () => {
    const dup = component('Dup', [prop('tone', 'Tone', { values: ['a-b', 'aB'] })]);
    expect(codeConnectSource(dup, entry('Dup'))).toContain(`{ 'A B': 'a-b', aB: 'aB' }`);
  });

  it('is deterministic whatever the prop order in the manifest', () => {
    const reversed = { ...BUTTON, props: [...BUTTON.props].reverse() };
    expect(codeConnectSource(reversed, entry('Button'))).toBe(source);
  });

  it('refuses a name that cannot be imported', () => {
    expect(() => codeConnectSource(component('Not-A-Name', []), entry('Not-A-Name'))).toThrow(
      FigmaConnectError,
    );
  });
});

describe('generateFigmaConnect', () => {
  it('writes one file per mapped component, sorted, creating the folder', () => {
    const out = join(tempDir(), 'nested/figma');
    const result = generateFigmaConnect(
      MANIFEST,
      { components: [entry('Divider'), entry('Button'), entry('AiComposer')] },
      out,
    );
    expect(result.written).toEqual([
      'AiComposer.figma.tsx',
      'Button.figma.tsx',
      'Divider.figma.tsx',
    ]);
    expect(result.skipped).toEqual([]);
    expect(readFileSync(join(out, 'Button.figma.tsx'), 'utf8')).toBe(
      codeConnectSource(BUTTON, entry('Button')),
    );
  });

  it('skips components that are not in the manifest, hooks and utilities', () => {
    const out = tempDir();
    const result = generateFigmaConnect(
      MANIFEST,
      { components: [entry('Nope'), entry('useToast'), entry('formatDate')] },
      out,
    );
    expect(result.written).toEqual([]);
    expect(result.skipped).toEqual([
      { component: 'Nope', reason: 'not in the component manifest' },
      { component: 'formatDate', reason: 'is a utility, not a component' },
      { component: 'useToast', reason: 'is a hook, not a component' },
    ]);
    expect(readdirSync(out)).toEqual([]);
  });

  it('keeps existing files unless force is set', () => {
    const out = tempDir();
    write(out, 'Button.figma.tsx', '// edited by hand\n');
    const map = { components: [entry('Button')] };
    const kept = generateFigmaConnect(MANIFEST, map, out);
    expect(kept.written).toEqual([]);
    expect(kept.skipped[0]?.reason).toContain('exists');
    expect(readFileSync(join(out, 'Button.figma.tsx'), 'utf8')).toBe('// edited by hand\n');
    const forced = generateFigmaConnect(MANIFEST, map, out, { force: true });
    expect(forced.written).toEqual(['Button.figma.tsx']);
    expect(readFileSync(join(out, 'Button.figma.tsx'), 'utf8')).toContain('figma.connect(Button');
  });

  it('never writes a file for a placeholder or invalid URL, even without parseFigmaMap', () => {
    const out = join(tempDir(), 'figma');
    const result = generateFigmaConnect(
      MANIFEST,
      { components: [entry('Button', { url: PLACEHOLDER_URL }), entry('Divider', { url: 'x' })] },
      out,
    );
    expect(result.written).toEqual([]);
    expect(result.skipped.map((s) => s.component)).toEqual(['Button', 'Divider']);
    expect(result.skipped[0]?.reason).toContain('placeholder');
    expect(existsSync(out)).toBe(false);
  });

  it('writes a component once when the map names it twice under different spellings', () => {
    const out = tempDir();
    const result = generateFigmaConnect(
      MANIFEST,
      { components: [entry('Button'), entry('button')] },
      out,
    );
    expect(result.written).toEqual(['Button.figma.tsx']);
    expect(result.skipped).toEqual([{ component: 'button', reason: 'listed more than once' }]);
  });
});
