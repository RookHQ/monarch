import { Kbd } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './kbd.fixture.json';

const keys = fixture.data.keys as [string, string];

export default {
  id: 'kbd',
  title: 'Kbd',
  summary: 'Keyboard shortcut hint.',
  sections: {
    purpose: {
      summary: 'Shows a keyboard key or shortcut, e.g. ⌘K.',
      useWhen: ['Hinting a shortcut in a menu, tooltip or help text.'],
    },
    designPrinciples: { items: ['Semantic: renders a native `<kbd>` element.'] },
    apiReference: { source: 'typescript', notes: ['Native `<kbd>` attributes pass through.'] },
    typedProperties: { source: 'typescript' },
    variants: { items: [], notes: ['Kbd has no variants.'] },
    states: { items: [], notes: ['No states: it renders static content.'] },
    interaction: { items: [], notes: ['Not interactive.'] },
    keyboard: { items: [], notes: ['Not interactive; it only displays a key.'] },
    accessibility: {
      items: [
        'Screen readers read the key text; spell out symbols nearby if they are not obvious.',
      ],
    },
    responsive: { items: ['Inline element; no breakpoints.'] },
    theme: { items: ['Border, surface and text use Monarch semantic tokens.'] },
    examples: {
      items: [
        {
          title: 'Shortcut',
          render: () => (
            <>
              <Kbd>{keys[0]}</Kbd> <Kbd>{keys[1]}</Kbd>
            </>
          ),
        },
      ],
    },
    doDont: {
      do: ['Use one Kbd per key in a combination.'],
      dont: ['Do not use Kbd for code snippets; use `<code>`.'],
    },
    related: {
      items: [{ id: 'dropdown-menu', reason: 'Menu items show shortcuts with `shortcut`.' }],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: ['Renders a `<kbd>` element.'],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery'],
    },
  },
} satisfies ComponentDocs;
