import { useState, type ReactNode } from 'react';
import {
  Alert,
  Badge,
  Button,
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
  Kbd,
} from '@rookcom/monarch-react';
import { exportsByName, version, type ExportDoc } from '../lib/manifest.js';
import {
  DOC_SECTIONS,
  DOC_SECTION_TITLES,
  FAMILY_TITLES,
  componentPages,
  isIncomplete,
  pageById,
  registryById,
  type ComponentPageInfo,
  type DocSectionId,
  type Example,
} from '../lib/components.js';
import { href } from '../lib/router.js';

function Code({ code }: { code: string }) {
  const [copied, setCopied] = useState(false);
  return (
    <pre className="docs-code">
      <Button
        className="docs-code__copy"
        size="xs"
        onClick={() => {
          void navigator.clipboard?.writeText(code).then(() => {
            setCopied(true);
            setTimeout(() => setCopied(false), 1500);
          });
        }}
      >
        {copied ? 'Copied' : 'Copy'}
      </Button>
      <code>{code}</code>
    </pre>
  );
}

function ExampleBlock({ ex }: { ex: Example }) {
  return (
    <section aria-label={`Example: ${ex.title}`}>
      <h3 className="docs-prose">{ex.title}</h3>
      <div className={`docs-example${ex.stack ? ' docs-example--stack' : ''}`}>{ex.render()}</div>
      <Code code={ex.code} />
    </section>
  );
}

const List = ({ items }: { items?: ReactNode[] | undefined }) =>
  items?.length ? (
    <ul className="docs-prose">
      {items.map((item, i) => (
        <li key={i}>{item}</li>
      ))}
    </ul>
  ) : null;

const Notes = ({ notes }: { notes?: string[] | undefined }) =>
  notes?.length ? (
    <>
      {notes.map((n) => (
        <p key={n} className="docs-prose">
          {n}
        </p>
      ))}
    </>
  ) : null;

/** Renders `code` spans written with backticks in docs strings. */
function Inline({ text }: { text: string }) {
  return (
    <>
      {text.split(/(`[^`]+`)/).map((part, i) =>
        part.startsWith('`') && part.endsWith('`') ? (
          <code key={i} className="docs-code-inline">
            {part.slice(1, -1)}
          </code>
        ) : (
          part
        ),
      )}
    </>
  );
}
const inline = (items?: string[]) => items?.map((t) => <Inline key={t} text={t} />);

function Table({ head, rows }: { head: string[]; rows: ReactNode[][] }) {
  return (
    <table className="docs-table">
      <thead>
        <tr>
          {head.map((h) => (
            <th key={h} scope="col">
              {h}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((r, i) => (
          <tr key={i}>
            {r.map((c, j) => (
              <td key={j}>{c}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function ComponentLink({ id }: { id: string }) {
  const page = pageById[id];
  if (page) return <a href={href(`/components/${id}`)}>{page.docs.title}</a>;
  const entry = registryById[id];
  return (
    <span>
      {entry?.name ?? id} <span className="docs-footnote">({entry?.status ?? 'unknown'})</span>
    </span>
  );
}

function ApiReference({ page, notes }: { page: ComponentPageInfo; notes?: string[] | undefined }) {
  const parts = page.exports.map((n) => exportsByName[n]).filter((p): p is ExportDoc => Boolean(p));
  return (
    <>
      <Code code={`import { ${page.exports.join(', ')} } from '@rookcom/monarch-react';`} />
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

function Properties({ page, notes }: { page: ComponentPageInfo; notes?: string[] | undefined }) {
  const parts = page.exports
    .map((n) => exportsByName[n])
    .filter((p): p is ExportDoc => Boolean(p) && p?.kind === 'component');
  return (
    <>
      <Notes notes={notes} />
      {parts.map((p) => (
        <section key={p.name} aria-labelledby={`api-${p.name}`}>
          <h3 id={`api-${p.name}`} className="docs-prose">
            {p.name}
          </h3>
          {p.description ? <p className="docs-prose">{p.description}</p> : null}
          {p.props.length ? (
            <Table
              head={['Prop', 'Type', 'Description']}
              rows={p.props.map((prop) => [
                <>
                  <code className="docs-code-inline">{prop.name}</code>
                  {prop.required ? <span className="docs-footnote"> required</span> : null}
                </>,
                <code key="t" className="docs-code-inline">
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
function SectionBody({
  id,
  s,
  page,
}: {
  id: DocSectionId;
  s: Record<string, unknown>;
  page: ComponentPageInfo;
}) {
  switch (id) {
    case 'purpose': {
      const p = s as { summary?: string; useWhen?: string[]; avoidWhen?: string[] };
      return (
        <>
          {p.summary ? (
            <p className="docs-prose">
              <Inline text={p.summary} />
            </p>
          ) : null}
          {p.useWhen?.length ? (
            <>
              <h3 className="docs-prose">Use when</h3>
              <List items={inline(p.useWhen)} />
            </>
          ) : null}
          {p.avoidWhen?.length ? (
            <>
              <h3 className="docs-prose">Avoid when</h3>
              <List items={inline(p.avoidWhen)} />
            </>
          ) : null}
        </>
      );
    }
    case 'apiReference':
      return <ApiReference page={page} notes={(s.notes as string[]) ?? []} />;
    case 'typedProperties':
      return <Properties page={page} notes={(s.notes as string[]) ?? []} />;
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
                v.values.map((x) => `'${x}'`).join(' | ') || '–',
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
            <Table
              head={['Role', 'Accessible name']}
              rows={[
                [
                  <Inline key="r" text={(s.role as string) ?? '–'} />,
                  <Inline key="n" text={(s.name as string) ?? '–'} />,
                ],
              ]}
            />
          ) : null}
          <List items={inline(s.items as string[])} />
        </>
      );
    case 'examples':
      return (
        <>
          {((s.items as Example[]) ?? []).map((ex) => (
            <ExampleBlock key={ex.title} ex={ex} />
          ))}
        </>
      );
    case 'doDont':
      return (
        <div className="docs-dodont">
          <section aria-label="Do">
            <h3 className="docs-prose">Do</h3>
            <List items={inline(s.do as string[])} />
          </section>
          <section aria-label="Don't">
            <h3 className="docs-prose">Don't</h3>
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
                pk.length
                  ? pk.map((p, i) => (
                      <span key={p}>
                        {i ? ', ' : ''}
                        <code className="docs-code-inline">{p}</code>
                      </span>
                    ))
                  : 'None',
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

export function ComponentsIndexPage() {
  const families = Object.keys(FAMILY_TITLES).filter((f) =>
    componentPages.some((p) => p.family === f),
  );
  return (
    <>
      <h1>Components</h1>
      <p className="docs-lead">
        {componentPages.length} components and patterns in{' '}
        <code className="docs-code-inline">@rookcom/monarch-react</code> {version}. Each page is
        generated from the component's docs file and its TypeScript declarations.
      </p>
      {families.map((f) => (
        <section key={f} aria-labelledby={`family-${f}`}>
          <h2 id={`family-${f}`} className="docs-h2">
            {FAMILY_TITLES[f]}
          </h2>
          <div className="docs-cards">
            {componentPages
              .filter((p) => p.family === f)
              .map((p) => (
                <Card key={p.docs.id}>
                  <CardHeader>
                    <CardTitle as="h3">
                      <a href={href(`/components/${p.docs.id}`)}>{p.docs.title}</a>
                    </CardTitle>
                    <CardDescription>{p.docs.summary}</CardDescription>
                  </CardHeader>
                </Card>
              ))}
          </div>
        </section>
      ))}
    </>
  );
}

export function ComponentPage({ id }: { id: string }) {
  const page = pageById[id];
  if (!page) {
    return (
      <>
        <h1>Unknown component</h1>
        <p className="docs-lead">
          <a href={href('/components')}>All components</a>
        </p>
      </>
    );
  }
  const { docs } = page;
  const sections = docs.sections as unknown as Record<DocSectionId, Record<string, unknown>>;
  return (
    <>
      <h1>{docs.title}</h1>
      <p className="docs-lead">{docs.summary}</p>
      {DOC_SECTIONS.map((sid) => {
        const s = sections[sid];
        const missing = isIncomplete(s as never);
        const body = missing ? ((s.partial as Record<string, unknown>) ?? {}) : s;
        return (
          <section key={sid} aria-labelledby={`section-${sid}`}>
            <h2 id={`section-${sid}`} className="docs-h2">
              {DOC_SECTION_TITLES[sid]}
            </h2>
            {missing ? (
              <Alert tone="warning" title="This section is incomplete">
                <Inline text={s.reason as string} />
              </Alert>
            ) : null}
            {!missing || s.partial || sid === 'apiReference' || sid === 'typedProperties' ? (
              <SectionBody id={sid} s={body} page={page} />
            ) : null}
          </section>
        );
      })}
    </>
  );
}
