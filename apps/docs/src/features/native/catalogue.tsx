import '../../styles/native.css';
import { Alert } from '@rookcom/monarch-react';
import type { CatalogueRow, NativeFacts } from '../../../plugins/native.js';
import { FAMILY_TITLES } from '../../lib/docs.js';
import { href } from '../../lib/router.js';
import { NEW_TAB } from '../../lib/external.js';
import { PageFrame, PageSection } from '../../layout/page-frame.js';
import { Table } from '../content/inline.js';
import { CoverageBadge, EntryStatusBadge, StatusBadge } from './badges.js';
import {
  COVERAGE,
  COVERAGE_ORDER,
  PATTERN,
  PLATFORM_NAMES,
  REPO_FILE,
  STATUS,
  STATUS_ORDER,
  type Platform,
} from './labels.js';

/** The catalogue group of the patterns layer (ADR 0017), listed after every family. */
const PATTERNS = 'patterns';
const FAMILY_ORDER = ['foundations', ...Object.keys(FAMILY_TITLES), PATTERNS];
const familyTitle = (family: string) =>
  family === 'foundations'
    ? 'Foundations'
    : family === PATTERNS
      ? 'Patterns (experimental)'
      : (FAMILY_TITLES[family] ?? family);
const groupOf = (row: CatalogueRow) => (row.pattern ? PATTERNS : row.family);

/** Where a row's documentation is: its component page, or the landing page for theme and icon. */
function docsHref(platform: Platform, row: CatalogueRow): string | null {
  if (row.web) return href(`/components/${row.id}`, platform);
  return row.entry ? href(`/${platform}/guide`, `reference-${row.id}`) : null;
}

function Family({
  platform,
  family,
  rows,
}: {
  platform: Platform;
  family: string;
  rows: CatalogueRow[];
}) {
  const name = PLATFORM_NAMES[platform];
  const inventory = rows.filter((r) => r.entry).length;
  return (
    <PageSection id={`family-${family}`} title={familyTitle(family)}>
      <p className="docs-footnote">
        {inventory} of {rows.length} in the {name} inventory.
      </p>
      <Table
        label={`${familyTitle(family)} on ${name}`}
        head={['Component', 'Native type', 'Coverage', 'Status']}
        rows={rows.map((row) => {
          const link = docsHref(platform, row);
          return [
            <span key="c">
              {link ? (
                <a className="docs-catalogue__name" href={link}>
                  {row.title}
                </a>
              ) : (
                <span className="docs-catalogue__name">{row.title}</span>
              )}
              {row.summary ? <span className="docs-catalogue__summary">{row.summary}</span> : null}
            </span>,
            row.name ? (
              <code key="n" className="docs-code-inline">
                {row.name}
              </code>
            ) : row.uses ? (
              <span key="n" className="docs-catalogue__uses">
                {row.uses}
              </span>
            ) : (
              '–'
            ),
            <CoverageBadge key="v" coverage={row.coverage} />,
            // Web-only rows have nothing to ship, so no status is claimed for them.
            row.coverage === 'web-only' ? (
              <span key="s" className="docs-footnote">
                Not applicable
              </span>
            ) : (
              <span key="s">
                <EntryStatusBadge status={row.status} pattern={row.pattern} />
                {row.since ? <span className="docs-footnote"> since {row.since}</span> : null}
              </span>
            ),
          ];
        })}
      />
    </PageSection>
  );
}

/**
 * A platform's component catalogue: every inventory entry and every component
 * that is only planned, by family, with coverage, status, native type and a
 * link to its documentation. All numbers are counted from
 * `platforms/<platform>/inventory` and `platforms/parity.json` at build time.
 */
export function NativeCatalogue({
  platform,
  rows,
  facts,
}: {
  platform: Platform;
  rows: readonly CatalogueRow[];
  facts: NativeFacts[Platform];
}) {
  const name = PLATFORM_NAMES[platform];
  const families = [...new Set(rows.map(groupOf))].sort(
    (a, b) =>
      (FAMILY_ORDER.indexOf(a) + 1 || FAMILY_ORDER.length + 1) -
        (FAMILY_ORDER.indexOf(b) + 1 || FAMILY_ORDER.length + 1) || a.localeCompare(b),
  );
  const other = facts.entries - facts.covered;
  const shippedStatuses = STATUS_ORDER.filter((s) => s !== 'planned' && (facts.status[s] ?? 0) > 0);
  return (
    <PageFrame
      eyebrow={name}
      title={`${name} components`}
      lead={`Every entry in the Monarch for ${name} inventory, and how the rest of the web catalogue is planned to be covered on ${name}. An entry is the platform's answer to a web component; it is not always a new component.`}
      wide
    >
      <PageSection id="progress" title="Progress">
        {facts.entries === 0 ? (
          <Alert tone="info" title={`The ${name} inventory has no entries yet`}>
            Everything listed below is the plan, not something that exists.
          </Alert>
        ) : null}
        <dl className="docs-facts">
          <div>
            <dt>Web components with an {name} entry</dt>
            <dd>
              {facts.covered}
              <span className="docs-facts__of"> of {facts.total}</span>
            </dd>
          </div>
          {shippedStatuses.map((s) => (
            <div key={s}>
              <dt>{STATUS[s].label}</dt>
              <dd>{facts.status[s]}</dd>
            </div>
          ))}
          <div>
            <dt>Planned, no entry yet</dt>
            <dd>{facts.total - facts.covered}</dd>
          </div>
        </dl>
        <p className="docs-footnote">
          Counted from <code className="docs-code-inline">platforms/{platform}/inventory</code> and{' '}
          <code className="docs-code-inline">platforms/parity.json</code> when this site was built
          {facts.version ? `, for Monarch for ${name} ${facts.version}` : ''}
          {facts.unreleased ? ' (unreleased)' : ''}.
          {other > 0
            ? ` ${other} more ${other === 1 ? 'entry has' : 'entries have'} no web component and ${other === 1 ? 'is' : 'are'} listed under Foundations.`
            : ''}
        </p>
      </PageSection>

      <PageSection id="legend" title="Coverage and status">
        <p className="docs-text">
          Parity means every web component has a defined answer on {name}, not that each one is
          rebuilt. The coverage says which kind of answer it is; the status says whether it exists.
        </p>
        <dl className="docs-deflist">
          {COVERAGE_ORDER.map((c) => (
            <div key={c}>
              <dt>{COVERAGE[c].label}</dt>
              <dd>
                {COVERAGE[c].meaning}
                {facts.coverage[c] !== undefined || facts.plan[c] !== undefined ? (
                  <span className="docs-footnote">
                    {' '}
                    {facts.coverage[c] ?? 0} in the inventory, {facts.plan[c] ?? 0} in the plan.
                  </span>
                ) : null}
              </dd>
            </div>
          ))}
        </dl>
        <dl className="docs-deflist">
          {STATUS_ORDER.map((s) => (
            <div key={s}>
              <dt>
                <StatusBadge status={s} />
              </dt>
              <dd>{STATUS[s].meaning}</dd>
            </div>
          ))}
          {rows.some((r) => r.pattern) ? (
            <div>
              <dt>
                <EntryStatusBadge status="experimental" pattern />
              </dt>
              <dd>{PATTERN.meaning}</dd>
            </div>
          ) : null}
        </dl>
        <p className="docs-footnote">
          The plan is explained in{' '}
          <a {...NEW_TAB} href={`${REPO_FILE}docs/architecture/NATIVE-PARITY.md`}>
            the native parity map
          </a>
          .
        </p>
      </PageSection>

      {families.map((family) => (
        <Family
          key={family}
          platform={platform}
          family={family}
          rows={rows.filter((r) => groupOf(r) === family)}
        />
      ))}
    </PageFrame>
  );
}
