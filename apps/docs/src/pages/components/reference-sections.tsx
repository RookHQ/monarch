/**
 * The reference sections of a component page, one renderer per section of the
 * docs-file contract (packages/react/src/internal/component-docs.ts). API and
 * properties come from the manifest generated from the TypeScript declarations.
 */
import { Badge, Kbd } from '@rookcom/monarch-react';
import type { ExportDoc } from '../../lib/manifest.js';
import {
  importLine,
  pageById,
  registryById,
  type DocSectionId,
  type DocsPageMeta,
} from '../../lib/docs.js';
import { href } from '../../lib/router.js';
import { CodeBlock } from '../../features/code/code-block.js';
import { Inline, List, Notes, Table, inline } from '../../features/content/inline.js';

function ComponentLink({ id }: { id: string }) {
  const page = pageById[id];
  if (page) return <a href={href(`/components/${id}`)}>{page.title}</a>;
  const entry = registryById[id];
  return (
    <span>
      {entry?.name ?? id} <span className="docs-footnote">({entry?.status ?? 'unknown'})</span>
    </span>
  );
}

interface ApiProps {
  page: DocsPageMeta;
  api: readonly ExportDoc[];
  notes?: string[] | undefined;
}

function ApiReference({ page, api: parts, notes }: ApiProps) {
  return (
    <>
      <CodeBlock code={importLine(page)} label="Code: import" />
      <Table
        head={['Export', 'Kind', 'Rendering', 'Root class']}
        rows={parts.map((p) => [
          <code key="n" className="docs-code-inline">
            {p.name}
          </code>,
          p.kind,
          p.clientOnly ? (
            <Badge size="sm">client</Badge>
          ) : (
            <Badge size="sm" tone="success">
              server-safe
            </Badge>
          ),
          p.className ? <code className="docs-code-inline">.{p.className}</code> : '–',
        ])}
      />
      <Notes notes={notes} />
      <p className="docs-footnote">
        Generated from the TypeScript declarations of{' '}
        <code className="docs-code-inline">packages/react/src/{page.folder}</code>.
      </p>
    </>
  );
}

function Properties({ api, notes }: ApiProps) {
  const parts = api.filter((p) => p.kind === 'component');
  return (
    <>
      <Notes notes={notes} />
      {parts.map((p) => (
        <section key={p.name} className="docs-props" aria-labelledby={`api-${p.name}`}>
          <h3 id={`api-${p.name}`} className="docs-subtitle">
            <code className="docs-code-inline">{p.name}</code>
          </h3>
          {p.description ? <p className="docs-text">{p.description}</p> : null}
          {p.props.length ? (
            <Table
              head={['Prop', 'Type', 'Description']}
              rows={p.props.map((prop) => [
                <span key="p" className="docs-prop-name">
                  <code className="docs-code-inline">{prop.name}</code>
                  {prop.required ? <span className="docs-footnote"> required</span> : null}
                </span>,
                <code key="t" className="docs-code-inline docs-code-inline--type">
                  {prop.type}
                </code>,
                prop.description,
              ])}
            />
          ) : (
            <p className="docs-footnote">Accepts the underlying element's props.</p>
          )}
        </section>
      ))}
    </>
  );
}

/** Content of one section; `s` is the complete section or an incomplete one's partial. */
export function SectionBody({
  id,
  s,
  page,
  api,
}: {
  id: Exclude<DocSectionId, 'examples'>;
  s: Record<string, unknown>;
  page: DocsPageMeta;
  api: readonly ExportDoc[];
}) {
  switch (id) {
    case 'purpose': {
      const p = s as { summary?: string; useWhen?: string[]; avoidWhen?: string[] };
      return (
        <>
          {p.summary ? (
            <p className="docs-text">
              <Inline text={p.summary} />
            </p>
          ) : null}
          <div className="docs-usewhen">
            {p.useWhen?.length ? (
              <div>
                <h3 className="docs-subtitle">Use when</h3>
                <List items={inline(p.useWhen)} />
              </div>
            ) : null}
            {p.avoidWhen?.length ? (
              <div>
                <h3 className="docs-subtitle">Avoid when</h3>
                <List items={inline(p.avoidWhen)} />
              </div>
            ) : null}
          </div>
        </>
      );
    }
    case 'apiReference':
      return <ApiReference page={page} api={api} notes={(s.notes as string[]) ?? []} />;
    case 'typedProperties':
      return <Properties page={page} api={api} notes={(s.notes as string[]) ?? []} />;
    case 'variants': {
      const items = (s.items as { prop: string; values: string[]; description: string }[]) ?? [];
      return (
        <>
          {items.length ? (
            <Table
              head={['Prop', 'Values', 'Description']}
              rows={items.map((v) => [
                <code key="p" className="docs-code-inline">
                  {v.prop}
                </code>,
                v.values.length ? (
                  <span key="v" className="docs-values">
                    {v.values.map((x) => (
                      <code key={x} className="docs-code-inline">
                        {`'${x}'`}
                      </code>
                    ))}
                  </span>
                ) : (
                  '–'
                ),
                <Inline key="d" text={v.description} />,
              ])}
            />
          ) : null}
          <Notes notes={s.notes as string[]} />
        </>
      );
    }
    case 'states': {
      const items = (s.items as { name: string; description: string; attribute?: string }[]) ?? [];
      return (
        <>
          {items.length ? (
            <Table
              head={['State', 'Exposed as', 'Behaviour']}
              rows={items.map((v) => [
                v.name,
                v.attribute ? <code className="docs-code-inline">{v.attribute}</code> : '–',
                <Inline key="d" text={v.description} />,
              ])}
            />
          ) : null}
          <Notes notes={s.notes as string[]} />
        </>
      );
    }
    case 'keyboard': {
      const items = (s.items as { keys: string; action: string }[]) ?? [];
      return (
        <>
          {items.length ? (
            <Table
              head={['Keys', 'Action']}
              rows={items.map((k) => [
                <Kbd key="k">{k.keys}</Kbd>,
                <Inline key="a" text={k.action} />,
              ])}
            />
          ) : null}
          <Notes notes={s.notes as string[]} />
        </>
      );
    }
    case 'accessibility':
      return (
        <>
          {s.role || s.name ? (
            <dl className="docs-deflist">
              <div>
                <dt>Role</dt>
                <dd>
                  <Inline text={(s.role as string) ?? '–'} />
                </dd>
              </div>
              <div>
                <dt>Accessible name</dt>
                <dd>
                  <Inline text={(s.name as string) ?? '–'} />
                </dd>
              </div>
            </dl>
          ) : null}
          <List items={inline(s.items as string[])} />
        </>
      );
    case 'doDont':
      return (
        <div className="docs-dodont">
          <section className="docs-dodont__card" data-kind="do" aria-labelledby={`${page.id}-do`}>
            <h3 id={`${page.id}-do`} className="docs-dodont__title">
              Do
            </h3>
            <List items={inline(s.do as string[])} />
          </section>
          <section
            className="docs-dodont__card"
            data-kind="dont"
            aria-labelledby={`${page.id}-dont`}
          >
            <h3 id={`${page.id}-dont`} className="docs-dodont__title">
              Don't
            </h3>
            <List items={inline(s.dont as string[])} />
          </section>
        </div>
      );
    case 'related':
      return (
        <List
          items={((s.items as { id: string; reason: string }[]) ?? []).map((r) => (
            <>
              <ComponentLink id={r.id} />: <Inline text={r.reason} />
            </>
          ))}
        />
      );
    case 'dependencies': {
      const pk = (s.packages as string[]) ?? [];
      const cm = (s.components as string[]) ?? [];
      return (
        <>
          <Table
            head={['Kind', 'Depends on']}
            rows={[
              [
                'npm packages',
                pk.length ? (
                  <span key="p" className="docs-values">
                    {pk.map((p) => (
                      <code key={p} className="docs-code-inline">
                        {p}
                      </code>
                    ))}
                  </span>
                ) : (
                  'None'
                ),
              ],
              [
                'Monarch components',
                cm.length
                  ? cm.map((c, i) => (
                      <span key={c}>
                        {i ? ', ' : ''}
                        <ComponentLink id={c} />
                      </span>
                    ))
                  : 'None',
              ],
            ]}
          />
          <Notes notes={s.notes as string[]} />
        </>
      );
    }
    case 'testing':
      return (
        <>
          <List items={inline(s.unit as string[])} />
          <Table
            head={['Check', 'Covered']}
            rows={[
              ['Axe in jsdom', s.axe ? 'Yes' : 'No'],
              ['Server rendering', s.ssr ? 'Yes' : 'No'],
              ['Browser suites', ((s.browser as string[]) ?? []).join('; ') || 'None yet'],
            ]}
          />
          <p className="docs-footnote">
            Tests: <code className="docs-code-inline">packages/react/src/{page.folder}</code>
          </p>
        </>
      );
    default:
      return (
        <>
          <List items={inline(s.items as string[])} />
          <Notes notes={s.notes as string[]} />
        </>
      );
  }
}
