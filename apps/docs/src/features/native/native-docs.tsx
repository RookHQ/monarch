import '../../styles/native.css';
import { Badge } from '@rookcom/monarch-react';
import type { NativePlan, NativePlatformDocs } from '../../../plugins/native.js';
import { pageById } from '../../lib/docs.js';
import { href } from '../../lib/router.js';
import { NEW_TAB } from '../../lib/external.js';
import { Inline, List, Table, inline } from '../content/inline.js';
import { CoverageBadge, EntryStatusBadge, StatusBadge } from './badges.js';
import { API_REFERENCE, COVERAGE, PLATFORM_NAMES, REPO_FILE, type Platform } from './labels.js';
import { NativeCode } from './native-code.js';
import { NativePreview } from './native-preview.js';

/** True when there is something to say about the component on the platform. */
export const hasNativeDocs = (docs: NativePlatformDocs | undefined): docs is NativePlatformDocs =>
  Boolean(docs && (docs.entry || docs.plan));

/** An example as it is compiled: the imports it names, its setup, then the body. */
export function exampleCode(example: {
  code: string;
  setup?: string | undefined;
  imports?: string[] | undefined;
}): string {
  return [example.imports?.map((i) => `import ${i}`).join('\n'), example.setup, example.code]
    .filter(Boolean)
    .join('\n\n');
}

/** A component that exists only in the parity plan: never shown as available. */
function Planned({
  platform,
  title,
  plan,
}: {
  platform: Platform;
  title: string;
  plan: NativePlan;
}) {
  const name = PLATFORM_NAMES[platform];
  const webOnly = plan.coverage === 'web-only';
  return (
    <div className="docs-native">
      <div className="docs-native__meta">
        {webOnly ? null : <StatusBadge status="planned" />}
        <CoverageBadge coverage={plan.coverage} />
      </div>
      <p className="docs-text">
        {webOnly
          ? `The parity plan marks ${title} as web only: Monarch for ${name} has no equivalent and none is planned.`
          : `${title} is not in Monarch for ${name} yet. What follows is the plan for it, not a description of something that exists.`}
      </p>
      <dl className="docs-deflist">
        <div>
          <dt>Planned coverage</dt>
          <dd>
            {COVERAGE[plan.coverage].label}. {COVERAGE[plan.coverage].meaning}
          </dd>
        </div>
        {plan.name ? (
          <div>
            <dt>Planned type</dt>
            <dd>
              <code className="docs-code-inline">{plan.name}</code>
            </dd>
          </div>
        ) : null}
        {plan.uses ? (
          <div>
            <dt>Platform API</dt>
            <dd>{plan.uses}</dd>
          </div>
        ) : null}
        {plan.note ? (
          <div>
            <dt>{webOnly ? 'Reason' : 'Notes'}</dt>
            <dd>
              <Inline text={plan.note} />
            </dd>
          </div>
        ) : null}
        {plan.wave !== null && !webOnly ? (
          <div>
            <dt>Build order</dt>
            <dd>Wave {plan.wave} of the plan</dd>
          </div>
        ) : null}
      </dl>
      <p className="docs-footnote">
        From{' '}
        <a {...NEW_TAB} href={`${REPO_FILE}docs/architecture/NATIVE-PARITY.md`}>
          the native parity map
        </a>{' '}
        (<code className="docs-code-inline">platforms/parity.json</code>).
      </p>
    </div>
  );
}

/**
 * One platform's documentation of a component, rendered from its inventory
 * entry (`platforms/<platform>/inventory/<id>.json`): status, platform
 * behaviour, guidance, API, options, states, appearance, accessibility, edge
 * cases, compiled examples and tests. Without an entry, the plan.
 */
export function NativeDocs({
  platform,
  id,
  title,
  docs,
  level = 3,
}: {
  platform: Platform;
  /** Prefix of the heading ids: the section's own id. */
  id: string;
  title: string;
  docs: NativePlatformDocs;
  /** Level of the headings inside: 3 under a page section, 4 under a subsection. */
  level?: 3 | 4;
}) {
  const { entry, plan } = docs;
  if (!entry) return plan ? <Planned platform={platform} title={title} plan={plan} /> : null;
  const name = PLATFORM_NAMES[platform];
  const H = level === 3 ? 'h3' : 'h4';
  const heading = (key: string, text: string) => (
    <H id={`${id}-${key}`} className="docs-subtitle">
      {text}
    </H>
  );
  const webOnly = entry.coverage === 'web-only';
  return (
    <div className="docs-native">
      <div className="docs-native__meta">
        {webOnly ? null : <EntryStatusBadge status={entry.status} pattern={entry.pattern} />}
        <CoverageBadge coverage={entry.coverage} />
        {entry.since ? (
          <Badge size="sm" variant="outline">
            {`Since ${entry.since}`}
          </Badge>
        ) : null}
        {entry.name ? <code className="docs-code-inline">{entry.name}</code> : null}
      </div>
      <p className="docs-text">
        <Inline text={entry.summary} />
      </p>
      <NativePreview
        platform={platform}
        title={title}
        previews={docs.previews}
        fallback={!webOnly}
      />

      {entry.wrapsNative || entry.platformNotes || entry.parityExceptions.length ? (
        <>
          {heading('platform', 'Platform behaviour')}
          {entry.wrapsNative ? (
            <dl className="docs-deflist">
              <div>
                <dt>Platform API</dt>
                <dd>
                  <code className="docs-code-inline">{entry.wrapsNative}</code>
                </dd>
              </div>
            </dl>
          ) : null}
          {entry.platformNotes ? (
            <p className="docs-text">
              <Inline text={entry.platformNotes} />
            </p>
          ) : null}
          {entry.parityExceptions.length ? (
            <>
              <p className="docs-text">Where this platform differs from the web on purpose:</p>
              <ul className="docs-list">
                {entry.parityExceptions.map((x) => (
                  <li key={`${x.rule}:${x.reason}`}>
                    <Inline text={x.reason} /> <Inline text={x.convention} />
                  </li>
                ))}
              </ul>
            </>
          ) : null}
        </>
      ) : null}

      {entry.whenToUse || entry.whenNotToUse ? (
        <div className="docs-usewhen">
          {entry.whenToUse ? (
            <div>
              {heading('use', 'When to use')}
              <p className="docs-text">
                <Inline text={entry.whenToUse} />
              </p>
            </div>
          ) : null}
          {entry.whenNotToUse ? (
            <div>
              {heading('avoid', 'When not to use')}
              <p className="docs-text">
                <Inline text={entry.whenNotToUse} />
              </p>
            </div>
          ) : null}
        </div>
      ) : null}

      {entry.api.length ? (
        <>
          {heading('api', 'API')}
          {entry.status === 'experimental' ? (
            <p className="docs-text">
              {platform === 'ios'
                ? 'Experimental API is visible only to a file that writes @_spi(Experimental) import MonarchUI. It can change in any release.'
                : 'Experimental API needs @OptIn(ExperimentalMonarchApi::class). It can change in any release.'}
            </p>
          ) : null}
          <NativeCode
            platform={platform}
            code={entry.api.join('\n\n')}
            label={`Code: ${name} API of ${title}`}
          />
          <p className="docs-text">
            The generated reference of every public declaration is the{' '}
            <a href={API_REFERENCE[platform].path} {...NEW_TAB}>
              {name} API reference ({API_REFERENCE[platform].tool})
            </a>
            {platform === 'ios' && entry.status === 'experimental'
              ? '. DocC leaves out experimental declarations; this section lists them.'
              : '.'}
          </p>
        </>
      ) : null}

      {entry.options.length ? (
        <>
          {heading('options', 'Options')}
          <Table
            label={`${title} options on ${name}`}
            head={['Option', 'Type', 'Default', 'Description']}
            rows={entry.options.map((o) => [
              <code key="n" className="docs-code-inline">
                {o.name}
              </code>,
              <code key="t" className="docs-code-inline docs-code-inline--type">
                {o.type}
              </code>,
              o.default === 'required' ? (
                <span key="d" className="docs-footnote">
                  required
                </span>
              ) : (
                <code key="d" className="docs-code-inline">
                  {o.default}
                </code>
              ),
              <Inline key="x" text={o.description} />,
            ])}
          />
        </>
      ) : null}

      {entry.states.length ? (
        <>
          {heading('states', 'States')}
          <Table
            label={`${title} states on ${name}`}
            head={['State', 'Description']}
            rows={entry.states.map((s) => [s.name, <Inline key="d" text={s.description} />])}
          />
        </>
      ) : null}

      {entry.appearance ? (
        <>
          {heading('appearance', 'Light, dark and high contrast')}
          <p className="docs-text">
            <Inline text={entry.appearance} />
          </p>
        </>
      ) : null}

      {entry.accessibility.length ? (
        <>
          {heading('accessibility', 'Accessibility')}
          <List items={inline(entry.accessibility)} />
        </>
      ) : null}

      {entry.edgeCases.length ? (
        <>
          {heading('edge-cases', 'Edge cases')}
          <List items={inline(entry.edgeCases)} />
        </>
      ) : null}

      {entry.examples.length ? (
        <>
          {heading('examples', 'Examples')}
          {entry.examples.map((example, i) => (
            <figure key={i} className="docs-native__example">
              <figcaption className="docs-native__caption">{example.title}</figcaption>
              <NativeCode
                platform={platform}
                code={exampleCode(example)}
                label={`Code: ${name} example ${i + 1}, ${example.title}`}
              />
            </figure>
          ))}
          <p className="docs-footnote">
            From the inventory entry. The {name} test suite compiles and renders every example.
          </p>
        </>
      ) : null}

      {entry.tests.length ? (
        <>
          {heading('tests', 'Tests')}
          <p className="docs-text">
            Covered by{' '}
            {entry.tests.map((t, i) => (
              <span key={t}>
                {i > 0 ? ', ' : null}
                <code className="docs-code-inline">{t}</code>
              </span>
            ))}
            .
          </p>
        </>
      ) : null}

      {entry.dependsOn.length ? (
        <p className="docs-text">
          Builds on:{' '}
          {entry.dependsOn.map((dep, i) => (
            <span key={dep}>
              {i > 0 ? ', ' : null}
              {pageById[dep] ? (
                <a href={href(`/components/${dep}`, platform)}>{pageById[dep].title}</a>
              ) : (
                <code className="docs-code-inline">{dep}</code>
              )}
            </span>
          ))}
          .
        </p>
      ) : null}
    </div>
  );
}
