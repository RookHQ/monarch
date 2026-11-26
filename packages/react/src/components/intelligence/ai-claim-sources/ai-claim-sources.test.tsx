import { createRef, useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AI_CLAIM_SOURCES_MESSAGES,
  AiClaimSources,
  aiClaimCounts,
  aiClaimNeedsChecking,
  aiClaimSummary,
  type AiClaim,
  type AiClaimSource,
} from './index.js';

const sources: AiClaimSource[] = [
  {
    id: 's1',
    title: 'Team handbook',
    url: 'https://example.com/handbook',
    kind: 'doc',
    publishedAt: '2026-03-03',
  },
  { id: 's2', title: 'Ticket 1042', kind: 'record' },
  { id: 's3', title: 'Old wiki page', url: 'javascript:alert(1)' },
];

const claims: AiClaim[] = [
  {
    id: 'c1',
    text: 'Requests are answered within two working days.',
    support: 'supported',
    evidence: [
      {
        id: 'e1',
        sourceId: 's1',
        quote: 'We answer every request within two working days.',
        location: 'Page 4',
        stance: 'supports',
      },
    ],
  },
  {
    id: 'c2',
    text: 'Urgent issues are handled within one hour.',
    support: 'conflicting',
    evidence: [
      { id: 'e2', sourceId: 's1', quote: 'Urgent issues: one hour.', stance: 'supports' },
      {
        id: 'e3',
        sourceId: 's2',
        quote: 'The urgent issue took six hours.',
        location: 'Line 12',
        stance: 'contradicts',
      },
    ],
  },
  { id: 'c3', text: 'Support is open at weekends.', support: 'unsupported', evidence: [] },
  {
    id: 'c4',
    text: 'Refunds take five days.',
    support: 'partial',
    evidence: [{ id: 'e4', sourceId: 's3', quote: 'Refunds are processed.' }],
  },
  { id: 'c5', text: 'The office moved in May.', support: 'unchecked', evidence: [] },
];

const trigger = (name: string | RegExp) => screen.getByRole('button', { name });

describe('aiClaimSummary and helpers', () => {
  it('counts claims by support level', () => {
    expect(aiClaimCounts(claims)).toEqual({
      total: 5,
      needsChecking: 4,
      supported: 1,
      partial: 1,
      unsupported: 1,
      conflicting: 1,
      unchecked: 1,
    });
    expect(aiClaimNeedsChecking({ support: 'supported' })).toBe(false);
    expect(aiClaimNeedsChecking({ support: 'unchecked' })).toBe(true);
  });

  it('writes the summary in plain words and leaves out empty levels', () => {
    const five: AiClaim['support'][] = [
      'supported',
      'supported',
      'supported',
      'partial',
      'unsupported',
    ];
    expect(aiClaimSummary(five.map((support) => ({ support })))).toBe(
      '5 claims: 3 supported, 1 partly, 1 without a source',
    );
    expect(aiClaimSummary([{ support: 'conflicting' }])).toBe(
      '1 claim: 1 with sources that disagree',
    );
    expect(aiClaimSummary([])).toBe('No claims');
    expect(aiClaimSummary(claims, (c) => `${c.total} Aussagen`)).toBe('5 Aussagen');
  });
});

describe('AiClaimSources', () => {
  it('is a labelled region listing every claim with its support in words and a passage count', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <AiClaimSources
        ref={ref}
        data-testid="root"
        className="extra"
        claims={claims}
        sources={sources}
      />,
    );
    const root = screen.getByRole('group', { name: 'Where this came from' });
    expect(root).toBe(ref.current);
    expect(root).toHaveClass('mn-ai-claim-sources', 'extra');
    expect(root).toHaveAttribute('data-testid', 'root');
    expect(
      screen.getByText(
        '5 claims: 1 supported, 1 partly, 1 without a source, 1 with sources that disagree, 1 not checked',
      ),
    ).toBeInTheDocument();
    const items = within(screen.getByRole('list', { name: 'Where this came from' })).getAllByRole(
      'listitem',
    );
    expect(items).toHaveLength(5);
    expect(items[0]).toHaveTextContent('Supported');
    expect(items[0]).toHaveTextContent('1 passage');
    expect(items[1]).toHaveTextContent('Sources disagree');
    expect(items[1]).toHaveTextContent('2 passages');
    // Never softened or hidden: stated on the collapsed row.
    expect(items[2]).toHaveTextContent('No source supports this');
    expect(items[2]).toHaveTextContent('No passages');
    expect(items[3]).toHaveTextContent('Partly supported');
    expect(items[4]).toHaveTextContent('Not checked yet');
    // The status describes the disclosure button.
    expect(trigger(/weekends/)).toHaveAccessibleDescription('No source supports this No passages');
    await expectNoA11yViolations();
  });

  it('opens a claim to show its passages as quotations with source, location, date and stance', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <AiClaimSources claims={claims} sources={sources} locale="en-GB" />,
    );
    const button = trigger(/two working days/);
    expect(button).toHaveAttribute('aria-expanded', 'false');
    expect(container.querySelector('blockquote')).toBeNull();
    await user.click(button);
    expect(button).toHaveAttribute('aria-expanded', 'true');
    expect(document.getElementById(button.getAttribute('aria-controls')!)).not.toBeNull();
    const list = screen.getByRole('list', {
      name: 'Passages for: Requests are answered within two working days.',
    });
    const quote = within(list).getByText('We answer every request within two working days.');
    expect(quote.tagName).toBe('BLOCKQUOTE');
    const caption = quote.closest('figure')!.querySelector('figcaption')!;
    expect(caption).toHaveTextContent('Team handbook');
    expect(caption).toHaveTextContent('Document');
    expect(caption).toHaveTextContent('Page 4');
    expect(caption).toHaveTextContent('Published 3 Mar 2026');
    expect(caption).toHaveTextContent('Supports the claim');
    expect(caption.querySelector('time')).toHaveAttribute('datetime', '2026-03-03');
    await expectNoA11yViolations();
    await user.click(button);
    expect(button).toHaveAttribute('aria-expanded', 'false');
  });

  it('says plainly when sources disagree or nothing supports a claim', async () => {
    render(
      <AiClaimSources claims={claims} sources={sources} defaultExpandedIds={['c2', 'c3', 'c5']} />,
    );
    expect(
      screen.getByText('Sources disagree about this claim. Read both sides before you rely on it.'),
    ).toBeInTheDocument();
    expect(screen.getByText('Contradicts the claim')).toBeInTheDocument();
    expect(screen.getByText('The urgent issue took six hours.').closest('li')).toHaveAttribute(
      'data-stance',
      'contradicts',
    );
    expect(
      screen.getByText('No source supports this claim. Check it yourself before you rely on it.'),
    ).toBeInTheDocument();
    expect(
      screen.getByText('This claim has not been checked against the sources yet.'),
    ).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('links to a safe URL, falls back to onOpenSource, and never links an unsafe URL', async () => {
    const onOpenSource = vi.fn();
    const user = userEvent.setup();
    const { rerender } = render(
      <AiClaimSources claims={claims} sources={sources} defaultExpandedIds={['c2', 'c4']} />,
    );
    const link = screen.getByRole('link', {
      name: 'Open source from Team handbook (opens in a new tab)',
    });
    expect(link).toHaveAttribute('href', 'https://example.com/handbook');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer nofollow');
    // No URL and an unsafe URL: nothing to open without a handler.
    expect(screen.getAllByRole('link')).toHaveLength(1);
    expect(screen.queryByRole('button', { name: /Open source/ })).toBeNull();

    rerender(
      <AiClaimSources
        claims={claims}
        sources={sources}
        defaultExpandedIds={['c2', 'c4']}
        onOpenSource={onOpenSource}
      />,
    );
    expect(screen.getAllByRole('link')).toHaveLength(1);
    await user.click(screen.getByRole('button', { name: 'Open source from Ticket 1042' }));
    expect(onOpenSource).toHaveBeenLastCalledWith(
      sources[1],
      claims[1]!.evidence[1],
      expect.anything(),
    );
    await user.click(screen.getByRole('button', { name: 'Open source from Old wiki page' }));
    expect(onOpenSource).toHaveBeenLastCalledWith(
      sources[2],
      claims[3]!.evidence[0],
      expect.anything(),
    );
  });

  it('lets the product take over a link by preventing its default', async () => {
    const user = userEvent.setup();
    const onOpenSource = vi.fn((_source, _evidence, event: { preventDefault: () => void }) =>
      event.preventDefault(),
    );
    render(
      <AiClaimSources
        claims={claims}
        sources={sources}
        defaultExpandedIds={['c1']}
        onOpenSource={onOpenSource}
      />,
    );
    await user.click(screen.getByRole('link', { name: /Open source/ }));
    expect(onOpenSource).toHaveBeenCalledTimes(1);
    expect(onOpenSource.mock.calls[0]![0]).toBe(sources[0]);
  });

  it('names an unknown source instead of dropping the passage', () => {
    render(
      <AiClaimSources
        claims={[
          {
            id: 'c',
            text: 'A claim.',
            support: 'supported',
            evidence: [{ id: 'e', sourceId: 'missing', quote: 'A passage.' }],
          },
        ]}
        sources={[]}
        defaultExpandedIds={['c']}
        onOpenSource={() => {}}
      />,
    );
    expect(screen.getByText('A passage.')).toBeInTheDocument();
    expect(screen.getByText('Unknown source')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Open source/ })).toBeNull();
  });

  it('copies the exact quote and reports it', async () => {
    const user = userEvent.setup();
    const onCopyQuote = vi.fn();
    render(
      <AiClaimSources
        claims={claims}
        sources={sources}
        defaultExpandedIds={['c1']}
        onCopyQuote={onCopyQuote}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Copy quote from Team handbook' }));
    expect(await screen.findByText('Quote copied to clipboard')).toBeInTheDocument();
    expect(await navigator.clipboard.readText()).toBe(
      'We answer every request within two working days.',
    );
    expect(onCopyQuote).toHaveBeenCalledWith(claims[0]!.evidence[0]);
  });

  it('filters to the claims that need checking and announces the result', async () => {
    const user = userEvent.setup();
    const onFilterChange = vi.fn();
    render(<AiClaimSources claims={claims} sources={sources} onFilterChange={onFilterChange} />);
    const group = screen.getByRole('radiogroup', { name: 'Show claims' });
    expect(within(group).getByRole('radio', { name: 'All 5' })).toBeChecked();
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
    await user.click(within(group).getByRole('radio', { name: 'Needs checking 4' }));
    expect(onFilterChange).toHaveBeenCalledWith('needs-checking');
    expect(screen.queryByRole('button', { name: /two working days/ })).toBeNull();
    expect(trigger(/weekends/)).toBeInTheDocument();
    expect(screen.getByText('Showing 4 of 5 claims')).toBeInTheDocument();
    // Arrow keys move the filter back (radio group).
    await user.keyboard('{ArrowLeft}');
    expect(trigger(/two working days/)).toBeInTheDocument();
  });

  it('supports a controlled filter and an empty "needs checking" result', () => {
    const onFilterChange = vi.fn();
    const supported = claims.slice(0, 1);
    const { rerender } = render(
      <AiClaimSources
        claims={supported}
        sources={sources}
        filter="all"
        onFilterChange={onFilterChange}
      />,
    );
    expect(trigger(/two working days/)).toBeInTheDocument();
    rerender(
      <AiClaimSources
        claims={supported}
        sources={sources}
        filter="needs-checking"
        onFilterChange={onFilterChange}
      />,
    );
    expect(screen.queryByRole('button', { name: /two working days/ })).toBeNull();
    expect(
      screen.getByText('Nothing needs checking: every claim is supported by a source.'),
    ).toBeInTheDocument();
  });

  it('controls which claims are open through expandedIds', async () => {
    const user = userEvent.setup();
    const onExpandedChange = vi.fn();
    const { rerender } = render(
      <AiClaimSources
        claims={claims}
        sources={sources}
        expandedIds={['c1']}
        onExpandedChange={onExpandedChange}
      />,
    );
    expect(trigger(/two working days/)).toHaveAttribute('aria-expanded', 'true');
    await user.click(trigger(/weekends/));
    expect(onExpandedChange).toHaveBeenLastCalledWith(['c1', 'c3']);
    // Controlled: nothing changes until the prop does.
    expect(trigger(/weekends/)).toHaveAttribute('aria-expanded', 'false');
    await user.click(trigger(/two working days/));
    expect(onExpandedChange).toHaveBeenLastCalledWith([]);
    rerender(
      <AiClaimSources
        claims={claims}
        sources={sources}
        expandedIds={['c3']}
        onExpandedChange={onExpandedChange}
      />,
    );
    expect(trigger(/weekends/)).toHaveAttribute('aria-expanded', 'true');
    expect(trigger(/two working days/)).toHaveAttribute('aria-expanded', 'false');
  });

  it('selects a claim when it is activated, uncontrolled and controlled', async () => {
    const user = userEvent.setup();
    const onSelectClaim = vi.fn();
    const { unmount } = render(
      <AiClaimSources claims={claims} sources={sources} onSelectClaim={onSelectClaim} />,
    );
    await user.click(trigger(/weekends/));
    expect(onSelectClaim).toHaveBeenCalledWith('c3');
    expect(trigger(/weekends/)).toHaveAttribute('aria-current', 'true');
    expect(trigger(/weekends/).closest('li')).toHaveAttribute('data-selected');
    unmount();

    function Controlled() {
      const [selected, setSelected] = useState<string | null>('c1');
      return (
        <>
          <AiClaimSources claims={claims} sources={sources} selectedClaimId={selected} />
          <button type="button" onClick={() => setSelected('c2')}>
            pick
          </button>
        </>
      );
    }
    render(<Controlled />);
    expect(trigger(/two working days/)).toHaveAttribute('aria-current', 'true');
    // Without the product updating the prop, activating another claim does not move the selection.
    await user.click(trigger(/weekends/));
    expect(trigger(/weekends/)).not.toHaveAttribute('aria-current');
    await user.click(screen.getByRole('button', { name: 'pick' }));
    expect(trigger(/one hour/)).toHaveAttribute('aria-current', 'true');
    expect(trigger(/two working days/)).not.toHaveAttribute('aria-current');
  });

  it('offers "Report a problem" on every open claim and "Check again" only on unchecked ones', async () => {
    const user = userEvent.setup();
    const onFlag = vi.fn();
    const onVerify = vi.fn();
    render(
      <AiClaimSources
        claims={claims}
        sources={sources}
        defaultExpandedIds={['c1', 'c5']}
        onFlag={onFlag}
        onVerify={onVerify}
      />,
    );
    expect(screen.getAllByRole('button', { name: 'Report a problem' })).toHaveLength(2);
    expect(screen.getAllByRole('button', { name: 'Check again' })).toHaveLength(1);
    await user.click(screen.getByRole('button', { name: 'Check again' }));
    expect(onVerify).toHaveBeenCalledWith('c5');
    await user.click(screen.getAllByRole('button', { name: 'Report a problem' })[0]!);
    expect(onFlag).toHaveBeenCalledWith('c1');
  });

  it('works from the keyboard in a logical order', async () => {
    const user = userEvent.setup();
    const onSelectClaim = vi.fn();
    render(
      <AiClaimSources
        claims={claims.slice(0, 3)}
        sources={sources}
        onSelectClaim={onSelectClaim}
        onFlag={() => {}}
      />,
    );
    await user.tab();
    expect(screen.getByRole('radio', { name: 'All 3' })).toHaveFocus();
    await user.tab();
    expect(trigger(/two working days/)).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(trigger(/two working days/)).toHaveAttribute('aria-expanded', 'true');
    expect(onSelectClaim).toHaveBeenCalledWith('c1');
    // The open claim's own actions come next, before the following claim.
    await user.tab();
    expect(screen.getByRole('link', { name: /Open source/ })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: /Copy quote/ })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Report a problem' })).toHaveFocus();
    await user.tab();
    expect(trigger(/one hour/)).toHaveFocus();
    // Arrow keys, Home and End move between claims without opening them.
    await user.keyboard('{ArrowDown}');
    expect(trigger(/weekends/)).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(trigger(/weekends/)).toHaveFocus();
    await user.keyboard('{Home}');
    expect(trigger(/two working days/)).toHaveFocus();
    await user.keyboard('{End}');
    expect(trigger(/weekends/)).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(trigger(/one hour/)).toHaveFocus();
    expect(trigger(/one hour/)).toHaveAttribute('aria-expanded', 'false');
    await user.keyboard(' ');
    expect(trigger(/one hour/)).toHaveAttribute('aria-expanded', 'true');
  });

  it('uses translated strings, including partial groups', () => {
    render(
      <AiClaimSources
        claims={claims}
        sources={sources}
        defaultExpandedIds={['c3']}
        onFlag={() => {}}
        messages={{
          heading: 'Woher das stammt',
          summary: (c) => `${c.total} Aussagen`,
          support: { unsupported: 'Keine Quelle belegt das' },
          notes: { unsupported: 'Keine Quelle belegt diese Aussage.' },
          filters: { 'needs-checking': 'Zu prüfen' },
          flag: 'Problem melden',
        }}
      />,
    );
    expect(screen.getByRole('group', { name: 'Woher das stammt' })).toBeInTheDocument();
    expect(screen.getByText('5 Aussagen')).toBeInTheDocument();
    expect(screen.getByText('Keine Quelle belegt das')).toBeInTheDocument();
    expect(screen.getByText('Keine Quelle belegt diese Aussage.')).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: 'Zu prüfen 4' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Problem melden' })).toBeInTheDocument();
    // Untranslated strings keep their English defaults.
    expect(screen.getByText(AI_CLAIM_SOURCES_MESSAGES.support.supported)).toBeInTheDocument();
  });

  it('shows an empty state without a filter when there are no claims', async () => {
    render(<AiClaimSources claims={[]} sources={[]} />);
    expect(screen.getByText('There are no claims to check.')).toBeInTheDocument();
    expect(screen.getByText('No claims')).toBeInTheDocument();
    expect(screen.queryByRole('radiogroup')).toBeNull();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiClaimSources
        claims={claims}
        sources={sources}
        defaultExpandedIds={['c1']}
        locale="en-GB"
      />,
    );
    expect(html).toContain('mn-ai-claim-sources');
    expect(html).toContain('<blockquote');
    expect(html).toContain('3 Mar 2026');
  });

  it('marks claim text and quoted passages dir="auto"', async () => {
    const user = userEvent.setup();
    const { container } = render(
      <AiClaimSources claims={claims} sources={sources} locale="en-GB" />,
    );
    expect(container.querySelector('.mn-ai-claim-sources__text')).toHaveAttribute('dir', 'auto');
    await user.click(trigger(/two working days/));
    expect(container.querySelector('blockquote')).toHaveAttribute('dir', 'auto');
  });
});
