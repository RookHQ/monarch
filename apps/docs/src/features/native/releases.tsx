import { Alert } from '@rookcom/monarch-react';
import type { NativeFacts } from '../../../plugins/native.js';
import { NEW_TAB } from '../../lib/external.js';
import { href } from '../../lib/router.js';
import { PageFrame, PageSection } from '../../layout/page-frame.js';
import { PLATFORM_NAMES, REPO_FILE, type Platform } from './labels.js';

/** A platform's release notes: its CHANGELOG.md, rendered at build time. */
export function NativeReleases({
  platform,
  changelog,
  facts,
}: {
  platform: Platform;
  changelog: { title: string; lead: string | null; html: string } | null;
  facts: NativeFacts[Platform];
}) {
  const name = PLATFORM_NAMES[platform];
  return (
    <PageFrame
      eyebrow={name}
      title={`${name} releases`}
      lead={
        changelog?.lead ? (
          <span dangerouslySetInnerHTML={{ __html: changelog.lead }} />
        ) : (
          `Release notes of Monarch for ${name}.`
        )
      }
    >
      {facts.unreleased ? (
        <Alert tone="warning" title="No version has been released">
          {facts.version
            ? `Version ${facts.version} is in development. The notes below describe what it contains so far.`
            : 'The notes below describe work in development.'}
        </Alert>
      ) : null}
      {changelog ? (
        <div className="docs-prose" dangerouslySetInnerHTML={{ __html: changelog.html }} />
      ) : (
        <p className="docs-text">
          <code className="docs-code-inline">platforms/{platform}/CHANGELOG.md</code> was not found
          when this site was built.
        </p>
      )}
      <PageSection id="policy" title="Versioning and migration">
        <p className="docs-text">
          Monarch for {name} has its own semantic version, independent of the npm packages and of
          the other native platform. A deprecated API keeps working for at least one minor version,
          names its replacement and states the earliest version that removes it.
          {facts.unreleased && facts.versions <= 1
            ? ' There is no migration guide yet, because there is no earlier version to migrate from.'
            : ''}
        </p>
        <ul className="docs-list">
          <li>
            <a {...NEW_TAB} href={`${REPO_FILE}docs/governance/native-api-stability.md`}>
              Native API stability
            </a>
            : what is public API, what counts as breaking, deprecation and the migration guide
            format.
          </li>
          <li>
            <a {...NEW_TAB} href={`${REPO_FILE}docs/governance/native-release-and-maintenance.md`}>
              Native release and maintenance
            </a>
            : versions, release tracks and the changelog standard.
          </li>
          <li>
            <a href={href(`/${platform}`)}>Monarch for {name}</a> and its{' '}
            <a href={href(`/${platform}/components`)}>component catalogue</a>.
          </li>
        </ul>
      </PageSection>
    </PageFrame>
  );
}
