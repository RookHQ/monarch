import '../../styles/component-page.css';
import { Alert, Badge } from '@rookcom/monarch-react';
import {
  DOC_SECTIONS,
  DOC_SECTION_TITLES,
  FAMILY_TITLES,
  examplesOf,
  isIncomplete,
  pageById,
  useDocs,
  type DocSectionId,
  type DocsPageMeta,
} from '../../lib/docs.js';
import { href } from '../../lib/router.js';
import { PageFrame, PageSection } from '../../layout/page-frame.js';
import { ExampleCard } from '../../features/examples/example-card.js';
import { Inline } from '../../features/content/inline.js';
import { SectionBody } from './reference-sections.js';

const REFERENCE = DOC_SECTIONS.filter(
  (s): s is Exclude<DocSectionId, 'examples'> => s !== 'examples',
);

/**
 * One page per docs file: the headline example big, the other examples with
 * "Show code", then every reference section of the docs contract.
 */
export function ComponentPage({ id }: { id: string }) {
  const meta = pageById[id];
  if (!meta) return <UnknownComponent />;
  return <Loaded meta={meta} />;
}

function Loaded({ meta }: { meta: DocsPageMeta }) {
  const { id } = meta;
  const { docs, source, api } = useDocs(id);
  const examples = examplesOf(docs);
  const codeOf = (i: number) => examples[i]?.code ?? source[i]?.code ?? '';
  const [first, ...rest] = examples;
  const sections = docs.sections as unknown as Record<DocSectionId, Record<string, unknown>>;
  const entry =
    meta.entry === '.' ? '@rookcom/monarch-react' : `@rookcom/monarch-react/${meta.entry.slice(2)}`;

  return (
    <PageFrame
      eyebrow={
        <>
          {meta.layer === 'patterns' ? 'Patterns' : 'Components'} ·{' '}
          {FAMILY_TITLES[meta.family] ?? meta.family}
        </>
      }
      title={docs.title}
      lead={docs.summary}
      meta={
        <>
          <Badge variant="outline" size="sm">
            {entry}
          </Badge>
          <Badge variant="subtle" size="sm" tone="neutral">
            {meta.layer === 'patterns' ? 'Pattern' : 'Component'}
          </Badge>
        </>
      }
    >
      {first ? <ExampleCard example={first} code={codeOf(0)} size="lg" headingLevel={2} /> : null}

      <PageSection id="examples" title={DOC_SECTION_TITLES.examples}>
        {rest.length ? (
          <div className="docs-examples">
            {rest.map((ex, i) => (
              <ExampleCard key={ex.title} example={ex} code={codeOf(i + 1)} />
            ))}
          </div>
        ) : (
          <p className="docs-text">
            {first
              ? 'The example at the top of the page is the only one for this component.'
              : 'No examples yet.'}
          </p>
        )}
      </PageSection>

      {REFERENCE.map((sid) => {
        const s = sections[sid];
        const missing = isIncomplete(s as never);
        const body = missing ? ((s.partial as Record<string, unknown>) ?? {}) : s;
        return (
          <PageSection key={sid} id={sid} title={DOC_SECTION_TITLES[sid]}>
            {missing ? (
              <Alert tone="warning" title="This section is incomplete">
                <Inline text={s.reason as string} />
              </Alert>
            ) : null}
            {!missing || s.partial || sid === 'apiReference' || sid === 'typedProperties' ? (
              <SectionBody id={sid} s={body} page={meta} api={api} />
            ) : null}
          </PageSection>
        );
      })}
    </PageFrame>
  );
}

function UnknownComponent() {
  return (
    <PageFrame title="Unknown component" toc={false}>
      <p className="docs-text">
        There is no component with that name. <a href={href('/components')}>See all components</a>.
      </p>
    </PageFrame>
  );
}
