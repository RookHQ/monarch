import {
  Button,
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'card',
  title: 'Card',
  summary: 'Grouped content surface.',
  sections: {
    purpose: {
      summary:
        'A bordered surface that groups related content, with optional header, content and footer parts.',
      useWhen: [
        'Grouping one topic on a page, such as a summary or a form section.',
        'Wrapping a table (`flush`).',
      ],
      avoidWhen: [
        'Showing a single KPI: use StatCard.',
        'A page-level settings group: use SettingsSection.',
      ],
    },
    designPrinciples: {
      items: [
        '`outline` (default) for most content, `raised` for floating panels, `subtle` inside other cards.',
        'CardTitle defaults to h3 and takes `as` so it fits the page outline.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Every part is a plain element (`div`, heading, `p`) and passes native attributes through.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['outline', 'raised', 'subtle'],
          description: 'Surface treatment. Default `outline`. Exposed as `data-variant`.',
        },
        {
          prop: 'flush',
          values: ['true', 'false'],
          description:
            'Removes the content padding, e.g. for a card wrapping a table (`data-flush`).',
        },
        {
          prop: 'CardTitle as',
          values: ['h2', 'h3', 'h4'],
          description: 'Heading level. Default `h3`.',
        },
      ],
    },
    states: {
      items: [{ name: 'Static', description: 'Card has no interactive states of its own.' }],
    },
    interaction: { items: ['None. Interactive content inside the card keeps its own behaviour.'] },
    keyboard: { items: [], notes: ['Not interactive, so there is no keyboard behaviour.'] },
    accessibility: {
      role: 'none (div); CardTitle is a heading',
      items: [
        'CardTitle renders a real heading (h3 by default); choose `as` so heading levels do not skip.',
        'The card adds no landmark or role; add `aria-labelledby` with a region role yourself if it needs one.',
      ],
    },
    responsive: { items: ['No breakpoints: the card fills the width its container gives it.'] },
    theme: {
      items: [
        'Borders and surfaces use Monarch semantic tokens, so every theme applies automatically.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Variants',
          render: () => (
            <>
              {(['outline', 'raised', 'subtle'] as const).map((v) => (
                <Card key={v} variant={v} style={{ width: '16rem' }}>
                  <CardHeader>
                    <CardTitle>Open tickets</CardTitle>
                    <CardDescription>{v} card</CardDescription>
                  </CardHeader>
                  <CardContent>128</CardContent>
                  <CardFooter>
                    <Button size="sm">View</Button>
                  </CardFooter>
                </Card>
              ))}
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Set CardTitle `as` to match the page outline.',
        'Use `flush` when a table fills the card.',
      ],
      dont: [
        'Do not nest `outline` cards; use `subtle` for an inner card.',
        'Do not make the whole card a click target without a real link or button inside.',
      ],
    },
    related: {
      items: [
        { id: 'stat-card', reason: 'A single KPI.' },
        { id: 'settings-section', reason: 'Titled settings groups.' },
        { id: 'section-card', reason: 'Planned card with a section header.' },
      ],
    },
    dependencies: { packages: [], components: [] },
    testing: {
      unit: ['Renders CardTitle as an h3 by default.'],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery'],
    },
  },
} satisfies ComponentDocs;
