/**
 * The component documentation contract (Monarch 0.4 directive §8).
 *
 * Every component folder has a `<id>.docs.tsx` whose default export satisfies
 * `ComponentDocs`. The docs site renders one page per file, and
 * `npm run components:check` fails when a folder has no docs file, a section
 * is missing, or a section is empty without being marked incomplete.
 *
 * Rules (docs/architecture/SOURCE-LAYOUT.md#docs-file-contract):
 * - Document only what the code does. When a section cannot be written
 *   truthfully yet, mark it `incomplete('why')` instead of guessing.
 * - API reference and typed properties are generated from the TypeScript
 *   declarations (dist/manifest.json); the docs file only adds notes.
 * - Live examples import components from '@rookcom/monarch-react', exactly as
 *   an app would, never from the folder's own files.
 *
 * This file is type-only apart from the `incomplete` helper and the section
 * list. It is excluded from the published build (tsconfig.build.json).
 */
import type { ReactNode } from 'react';

/** The 16 sections, in page order. */
export const DOC_SECTIONS = [
  'purpose',
  'designPrinciples',
  'apiReference',
  'typedProperties',
  'variants',
  'states',
  'interaction',
  'keyboard',
  'accessibility',
  'responsive',
  'theme',
  'examples',
  'doDont',
  'related',
  'dependencies',
  'testing',
] as const;
export type DocSectionId = (typeof DOC_SECTIONS)[number];

/** Human headings for each section, used by the docs site and the checker. */
export const DOC_SECTION_TITLES: Record<DocSectionId, string> = {
  purpose: 'Purpose',
  designPrinciples: 'Design principles',
  apiReference: 'API reference',
  typedProperties: 'Properties',
  variants: 'Variants',
  states: 'States',
  interaction: 'Interaction behaviour',
  keyboard: 'Keyboard support',
  accessibility: 'Accessibility',
  responsive: 'Responsive behaviour',
  theme: 'Theme support',
  examples: 'Usage examples',
  doDont: "Do and don't",
  related: 'Related components',
  dependencies: 'Dependencies',
  testing: 'Testing coverage',
};

/**
 * A section that cannot be filled truthfully yet (legacy components only).
 * Anything already known can still be given in `partial`; the page shows it
 * under a notice that the section is incomplete.
 */
export interface IncompleteSection<T> {
  status: 'legacy-incomplete';
  /** Why the section is incomplete and what would complete it. */
  reason: string;
  partial?: Partial<T>;
}

/** Marks a section incomplete. Only for components that existed before Monarch 0.4. */
export function incomplete<T>(reason: string, partial?: Partial<T>): IncompleteSection<T> {
  return partial
    ? { status: 'legacy-incomplete', reason, partial }
    : { status: 'legacy-incomplete', reason };
}

export type Section<T> = T | IncompleteSection<T>;

export interface Example {
  title: string;
  /** Stack the rendered example vertically instead of in an inline row. */
  stack?: boolean;
  /** The live demonstration. Import components from '@rookcom/monarch-react'. */
  render: () => ReactNode;
  /** The source shown under the demo. Keep it identical in behaviour to `render`. */
  code: string;
}

export interface PurposeSection {
  /** One or two sentences: what problem this component solves. */
  summary: string;
  useWhen: string[];
  avoidWhen?: string[];
}

/**
 * Every list-shaped section may have an empty `items` list when `notes` says
 * why (for example "Not interactive"). The checker rejects an empty list with
 * no notes.
 */
export interface ListSection {
  items: string[];
  notes?: string[];
}

export interface GeneratedSection {
  /** The content comes from the TypeScript declarations via dist/manifest.json. */
  source: 'typescript';
  /** Optional remarks the declarations cannot express. */
  notes?: string[];
}

export interface VariantsSection {
  items: { prop: string; values: string[]; description: string }[];
  notes?: string[];
}

export interface StatesSection {
  items: { name: string; description: string; attribute?: string }[];
  notes?: string[];
}

export interface KeyboardSection {
  /** Empty when the component is not interactive; say so in `notes`. */
  items: { keys: string; action: string }[];
  notes?: string[];
}

export interface AccessibilitySection {
  /** The ARIA role the component exposes, if any. */
  role?: string;
  /** Where the accessible name comes from. */
  name?: string;
  items: string[];
}

export interface ExamplesSection {
  items: Example[];
}

export interface DoDontSection {
  do: string[];
  dont: string[];
}

export interface RelatedSection {
  /** Registry ids (registry/components.json) with the reason to choose one over the other. */
  items: { id: string; reason: string }[];
}

export interface DependenciesSection {
  /** npm packages the component imports at runtime (checked against the source). */
  packages: string[];
  /** Other Monarch component folders it composes, by registry id (checked against the source). */
  components: string[];
  /** Monarch tokens the stylesheet relies on that are worth calling out. */
  notes?: string[];
}

export interface TestingSection {
  /** What the colocated `<id>.test.tsx` proves, one line per behaviour. */
  unit: string[];
  /** True when the test runs axe (expectNoA11yViolations) against the rendered component. */
  axe: boolean;
  /** True when the test renders the component with react-dom/server. */
  ssr: boolean;
  /** Where the component appears in the docs-site visual and cross-browser axe suites. */
  browser?: string[];
}

export interface ComponentDocsSections {
  purpose: Section<PurposeSection>;
  designPrinciples: Section<ListSection>;
  apiReference: Section<GeneratedSection>;
  typedProperties: Section<GeneratedSection>;
  variants: Section<VariantsSection>;
  states: Section<StatesSection>;
  interaction: Section<ListSection>;
  keyboard: Section<KeyboardSection>;
  accessibility: Section<AccessibilitySection>;
  responsive: Section<ListSection>;
  theme: Section<ListSection>;
  examples: Section<ExamplesSection>;
  doDont: Section<DoDontSection>;
  related: Section<RelatedSection>;
  dependencies: Section<DependenciesSection>;
  testing: Section<TestingSection>;
}

export interface ComponentDocs {
  /** The folder name, which is the registry id (registry/components.json). */
  id: string;
  /** Page title: the primary export's name. */
  title: string;
  /** One line for the components index card. */
  summary: string;
  sections: ComponentDocsSections;
}

export const isIncomplete = <T>(s: Section<T>): s is IncompleteSection<T> =>
  typeof s === 'object' && s !== null && (s as { status?: unknown }).status === 'legacy-incomplete';
