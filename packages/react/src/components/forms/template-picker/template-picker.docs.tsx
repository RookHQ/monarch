import { useState } from 'react';
import { TemplatePicker, type TemplatePickerTemplate } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

type Layout = 'sidebar' | 'hero' | 'grid' | 'list' | 'split' | 'form';

/** A tiny drawn layout in Monarch tokens, standing in for a product's thumbnail. */
function Thumb({ layout }: { layout: Layout }) {
  const line = (w: string, strong = false) => (
    <span
      style={{
        display: 'block',
        height: 4,
        width: w,
        borderRadius: 2,
        background: strong ? 'var(--mn-color-accent-solid)' : 'var(--mn-color-border-strong)',
      }}
    />
  );
  const box = (h: number) => (
    <span
      style={{
        display: 'block',
        height: h,
        borderRadius: 3,
        border: '1px solid var(--mn-color-border-default)',
        background: 'var(--mn-color-bg-surface)',
      }}
    />
  );
  const frame = {
    display: 'flex',
    gap: 6,
    width: '82%',
    height: '78%',
    padding: 6,
    boxSizing: 'border-box',
    borderRadius: 4,
    background: 'var(--mn-color-bg-surface)',
    border: '1px solid var(--mn-color-border-subtle)',
  } as const;
  const col = { display: 'flex', flexDirection: 'column', gap: 4, flex: 1 } as const;
  if (layout === 'sidebar')
    return (
      <span style={frame}>
        <span style={{ ...col, flex: '0 0 28%' }}>
          {line('70%', true)}
          {line('90%')}
          {line('60%')}
          {line('80%')}
        </span>
        <span style={col}>
          {box(14)}
          {line('80%')}
          {line('60%')}
        </span>
      </span>
    );
  if (layout === 'hero')
    return (
      <span style={{ ...frame, flexDirection: 'column' }}>
        <span
          style={{
            display: 'block',
            height: 18,
            borderRadius: 3,
            background: 'var(--mn-color-accent-subtle)',
          }}
        />
        {line('60%', true)}
        {line('85%')}
        {line('70%')}
      </span>
    );
  if (layout === 'grid')
    return (
      <span style={{ ...frame, display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)' }}>
        {[0, 1, 2, 3, 4, 5].map((i) => (
          <span key={i}>{box(16)}</span>
        ))}
      </span>
    );
  if (layout === 'split')
    return (
      <span style={frame}>
        <span style={{ flex: 1, borderRadius: 3, background: 'var(--mn-color-accent-subtle)' }} />
        <span style={col}>
          {line('70%', true)}
          {line('90%')}
          {line('50%')}
        </span>
      </span>
    );
  if (layout === 'form')
    return (
      <span style={{ ...frame, flexDirection: 'column' }}>
        {line('40%', true)}
        {box(8)}
        {box(8)}
        {box(16)}
      </span>
    );
  return (
    <span style={{ ...frame, flexDirection: 'column' }}>
      {line('50%', true)}
      {[0, 1, 2, 3].map((i) => (
        <span key={i}>{line(`${90 - i * 10}%`)}</span>
      ))}
    </span>
  );
}

/** Built lazily: docs files cannot create JSX at module scope. */
const pages = (): TemplatePickerTemplate[] => [
  {
    id: 'classic',
    name: 'Classic',
    description: 'Navigation on the side, content on the right.',
    category: 'pages',
    preview: <Thumb layout="sidebar" />,
    badge: 'Built-in',
  },
  {
    id: 'banner',
    name: 'Banner',
    description: 'A large header, then a list.',
    category: 'pages',
    preview: <Thumb layout="hero" />,
  },
  {
    id: 'gallery',
    name: 'Gallery',
    description: 'Cards in a grid.',
    category: 'pages',
    preview: <Thumb layout="grid" />,
  },
  {
    id: 'split',
    name: 'Split',
    description: 'Image beside text.',
    category: 'pages',
    preview: <Thumb layout="split" />,
  },
  {
    id: 'article',
    name: 'Article',
    description: 'One readable column.',
    category: 'docs',
    preview: <Thumb layout="list" />,
    keywords: ['blog', 'post'],
  },
  {
    id: 'intake',
    name: 'Intake form',
    description: 'Collect details before a call.',
    category: 'forms',
    preview: <Thumb layout="form" />,
  },
];

const CATEGORIES = [
  { id: 'pages', label: 'Pages' },
  { id: 'docs', label: 'Articles' },
  { id: 'forms', label: 'Forms' },
];

function GalleryDemo() {
  const [value, setValue] = useState<string | null>('classic');
  return (
    <TemplatePicker
      aria-label="Page template"
      templates={pages()}
      categories={CATEGORIES}
      blank={{ description: 'Start from an empty page.' }}
      value={value}
      onValueChange={(id) => setValue(id)}
      renderPreview={(t) => (
        <div style={{ display: 'grid', gap: 8 }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              aspectRatio: '4 / 3',
              borderRadius: 8,
              background: 'var(--mn-color-bg-muted)',
            }}
          >
            {t?.preview ?? <span style={{ color: 'var(--mn-color-text-muted)' }}>Empty page</span>}
          </div>
          <strong>{t ? t.name : 'Blank'}</strong>
          <span style={{ color: 'var(--mn-color-text-muted)', fontSize: 13 }}>
            {t ? t.description : 'Start from an empty page.'}
          </span>
        </div>
      )}
    />
  );
}

export default {
  id: 'template-picker',
  title: 'TemplatePicker',
  summary:
    'Choose a template from a gallery: category filters, search, thumbnails, a blank option, a preview pane and arrow-key grid navigation.',
  sections: {
    purpose: {
      summary:
        'One gallery for every “start from a template” choice: page layouts, issue and ticket templates, form starters. The product supplies the templates and their thumbnails and decides what choosing one does. Import from `@rookcom/monarch-react/patterns`.',
      useWhen: [
        'People pick one starting point from several visual options.',
        'The options benefit from thumbnails, categories or search.',
      ],
      avoidWhen: [
        'Two to five short text options: use SegmentedControl or a radio group.',
        'A long list of named items without visuals: use Select or Combobox.',
      ],
    },
    designPrinciples: {
      items: [
        'A real radio group: one choice, one Tab stop, arrows to move, so selection is announced.',
        'The thumbnail shows, the text tells: previews are decorative and every card has a name.',
        'Starting from scratch is always an option when `blank` is set, and it comes first.',
        'Filters narrow, they never hide the choice silently: the result count is announced and an empty result says so.',
        'The selected card is marked by an accent ring and a check, not colour alone.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<div>` (ref) with the filters, a `role="radiogroup"` of `role="radio"` card buttons, and an optional preview `<section aria-label="Preview">`.',
        'Templates are `{ id, name, description?, category?, preview?, badge?, keywords?, disabled? }`; `onValueChange(id, template)` receives `null` as the template for the blank card.',
        '`templateMatches(template, query)` is the search rule (every word must appear in the name, a string description or the keywords).',
        'Cards expose `data-state="checked|unchecked"` and, for the blank card, `data-blank`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'blank',
          values: ['false', 'true', '{ id, name, description, preview }'],
          description: 'Adds a dashed “start from scratch” card first (id `blank` by default).',
        },
        {
          prop: 'renderPreview',
          values: ['undefined', '(template) => ReactNode'],
          description:
            'A larger preview of the selected card: beside the grid from 48rem of width, below it otherwise.',
        },
        {
          prop: 'minCardWidth',
          values: ['9rem (default)', 'any CSS length'],
          description: 'The narrowest a card gets; the grid fits as many columns as that allows.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Selected',
          attribute: 'aria-checked="true", data-state="checked"',
          description: 'Accent ring and a check on the thumbnail.',
        },
        { name: 'Hover', description: 'Stronger border.' },
        { name: 'Focus', description: 'Focus ring outside the card.' },
        {
          name: 'Disabled',
          attribute: 'disabled',
          description: 'Faded and skipped by the arrow keys.',
        },
        {
          name: 'Category on',
          attribute: 'aria-pressed="true"',
          description: 'Accent-tinted filter chip.',
        },
        {
          name: 'No results',
          description: '“No templates match …” under the grid; the count is announced.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a card selects it and calls `onValueChange`. Arrow keys move and select, like any radio group.',
        'Category chips and search filter the cards; the blank card always stays.',
        'If the selected card is filtered out, the selection is kept and the Tab stop moves to the first visible card.',
        'Applying the template (filling fields, creating a page) is the product’s job, usually behind a Continue button.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves into the grid (to the selected card, or the first one) and out of it.',
        },
        {
          keys: 'Arrow Left / Right',
          action: 'Previous or next card, wrapping; mirrored in right-to-left.',
        },
        {
          keys: 'Arrow Up / Down',
          action: 'The card above or below in the grid, by the number of columns shown.',
        },
        { keys: 'Home / End', action: 'First or last card.' },
        { keys: 'Space / Enter', action: 'Selects the focused card.' },
      ],
      notes: [
        'Disabled cards are skipped. The search box and chips come before the grid in Tab order.',
      ],
    },
    accessibility: {
      role: 'radiogroup of radio',
      name: '`aria-label` (default “Templates”) or `aria-labelledby`.',
      items: [
        'Each card is named by the template name and described by its badge and description.',
        'Thumbnails and the check are `aria-hidden`; nothing depends on seeing them.',
        'The filter chips are toggle buttons (`aria-pressed`) in a group named “Categories”; the search box is a named searchbox.',
        'The result count is a visually hidden polite status that updates as filters change.',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/template-picker.',
      ],
    },
    responsive: {
      items: [
        'The grid uses as many columns as fit (`minCardWidth`), down to one at 320px.',
        'Filters wrap: the search box takes a full line on narrow screens, chips wrap below.',
        'The preview pane sits beside the grid from 48rem of the picker’s own width (container query) and below it otherwise.',
      ],
    },
    theme: {
      items: [
        'Monarch tokens only: `--mn-color-accent-solid` for the selected ring and check, `--mn-color-bg-subtle` behind thumbnails.',
        'Thumbnails are the product’s; draw them with tokens (as the examples do) so they follow light, dark and high contrast.',
        'Forced-colours mode outlines the selected card in Highlight; transitions stop under `prefers-reduced-motion`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Gallery with categories, search, blank and preview',
          stack: true,
          render: () => <GalleryDemo />,
        },
        {
          title: 'Compact, without search',
          stack: true,
          render: () => (
            <TemplatePicker
              aria-label="Issue template"
              searchable={false}
              minCardWidth="8rem"
              defaultValue="bug"
              templates={[
                {
                  id: 'bug',
                  name: 'Bug report',
                  description: 'Steps, expected, actual.',
                  badge: 'Built-in',
                },
                { id: 'feature', name: 'Feature request', description: 'Problem and proposal.' },
                { id: 'task', name: 'Task', description: 'A checklist to finish.' },
                {
                  id: 'spike',
                  name: 'Research',
                  description: 'A question and a time box.',
                  disabled: true,
                },
              ]}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Give every template a short, distinct name; the name is what screen readers announce.',
        'Draw thumbnails with tokens or the customer’s brand colour so they look like the real result.',
        'Confirm with a separate Continue or Use template button when choosing has side effects.',
      ],
      dont: [
        'Do not apply a template the moment a card is selected; arrow keys select as they move.',
        'Do not put essential information only in the thumbnail.',
        'Do not use it for more than a few dozen templates without categories and search.',
      ],
    },
    related: {
      items: [
        { id: 'segmented-control', reason: 'Two to five short text options.' },
        { id: 'select', reason: 'Many named options without visuals.' },
        { id: 'search-input', reason: 'The search box used here.' },
        { id: 'empty-state', reason: 'What to show when there are no templates at all.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['search-input'],
    },
    testing: {
      unit: [
        'Named radiogroup of radio cards, blank first, names and descriptions, disabled card, one Tab stop; axe.',
        'Click selects (uncontrolled) and reports the id and template; blank reports `null`.',
        'Arrow keys move and select, skip disabled cards and wrap; Home and End.',
        'Up and Down move by the number of columns (layout simulated in jsdom).',
        'Category chips and search filter; result count announced; no-results message; axe.',
        'Controlled value and the preview pane, including the blank card.',
        '`templateMatches` rules; translated messages; renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/template-picker',
        'Cross-browser axe: /components/template-picker',
      ],
    },
  },
} satisfies ComponentDocs;
