import { describe, expect, it, vi } from 'vitest';
import { createRef } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import { Avatar } from '../../../components/identity/avatar/index.js';
import { Badge } from '../../../components/data-display/badge/index.js';
import { BackLink } from '../../../components/navigation/back-link/index.js';
import { RecordHeader } from './index.js';

describe('RecordHeader', () => {
  it('renders identity, status, subtitle, facts, actions, banner and tabs', async () => {
    const ref = createRef<HTMLElement>();
    const { container } = render(
      <main>
        <RecordHeader
          ref={ref}
          title="Ada Lovelace"
          avatar={<Avatar name="Ada Lovelace" />}
          status={<Badge tone="success">Active</Badge>}
          badges={<Badge>Enterprise</Badge>}
          subtitle="ada@example.com"
          back={<BackLink href="/contacts">All contacts</BackLink>}
          facts={[
            { term: 'Owner', value: 'Grace Hopper' },
            { id: 'id', term: 'Contact ID', value: 'c_1042', copyable: true },
            { term: 'Phone', value: null },
          ]}
          actions={<Button>Edit</Button>}
          banner={<p role="status">Merged into Ada L.</p>}
          tabs={<nav aria-label="Contact sections">tabs</nav>}
        />
      </main>,
    );
    expect(ref.current).toBe(container.querySelector('header.mn-record-header'));
    expect(screen.getByRole('heading', { level: 1, name: 'Ada Lovelace' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'All contacts' })).toBeInTheDocument();
    expect(screen.getByText('Active').closest('.mn-record-header__badges')).not.toBeNull();
    expect(screen.getByText('ada@example.com')).toHaveClass('mn-record-header__subtitle');
    expect(screen.getByText('Owner').tagName).toBe('DT');
    expect(screen.getByText('Grace Hopper').tagName).toBe('DD');
    expect(screen.getByRole('button', { name: 'Copy Contact ID' })).toBeInTheDocument();
    expect(screen.getByText('Not set')).toBeInTheDocument();
    expect(screen.getByText('Merged into Ada L.').parentElement).toHaveClass(
      'mn-record-header__banner',
    );
    expect(screen.getByRole('navigation', { name: 'Contact sections' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('prev/next navigator: position text, labels, disabled ends, keyboard', async () => {
    const onNext = vi.fn();
    render(
      <RecordHeader
        title="Printer offline"
        headingLevel="h2"
        navigator={{
          position: { index: 0, total: 40 },
          onNext,
          previousLabel: 'Previous ticket',
          nextLabel: 'Next ticket',
        }}
      />,
    );
    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('Printer offline');
    expect(screen.getByText('1 of 40')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Previous ticket' })).toBeDisabled();
    const next = screen.getByRole('button', { name: 'Next ticket' });
    const user = userEvent.setup();
    await user.tab();
    expect(next).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onNext).toHaveBeenCalledOnce();
  });

  it('formats the position and hides it for an empty list', () => {
    const { rerender } = render(
      <RecordHeader
        title="T"
        navigator={{
          position: { index: 2, total: 9 },
          formatPosition: (i, t) => `Ticket ${i + 1} / ${t}`,
        }}
      />,
    );
    expect(screen.getByText('Ticket 3 / 9')).toBeInTheDocument();
    rerender(<RecordHeader title="T" navigator={{ position: { index: 0, total: 0 } }} />);
    expect(screen.queryByText(/of 0/)).toBeNull();
    expect(screen.getByRole('button', { name: 'Next record' })).toBeDisabled();
  });

  it('edits a text title in place: Enter saves, Escape cancels', async () => {
    const onTitleSave = vi.fn();
    render(
      <RecordHeader
        title="Printer offline"
        onTitleSave={onTitleSave}
        titleLabel="Subject"
        titleEditProps={{ maxLength: 120 }}
      />,
    );
    const heading = screen.getByRole('heading', { level: 1 });
    expect(heading.tagName).toBe('DIV');
    const view = screen.getByRole('button', { name: /subject/i });
    expect(heading).toContainElement(view);
    const user = userEvent.setup();
    await user.click(view);
    const input = await screen.findByRole('textbox', { name: 'Subject' });
    expect(input).toHaveAttribute('maxlength', '120');
    await user.clear(input);
    await user.type(input, 'Printer back online{Enter}');
    await waitFor(() => expect(onTitleSave).toHaveBeenCalledWith('Printer back online'));

    await user.click(screen.getByRole('button', { name: /subject/i }));
    await user.type(await screen.findByRole('textbox', { name: 'Subject' }), ' x{Escape}');
    expect(onTitleSave).toHaveBeenCalledOnce();
    await expectNoA11yViolations();
  });

  it('a non-text title stays a plain heading even with onTitleSave', () => {
    render(<RecordHeader title={<span>Rich title</span>} onTitleSave={() => {}} />);
    expect(screen.getByRole('heading', { level: 1 }).tagName).toBe('H1');
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <RecordHeader
        title="Acme Corp"
        onTitleSave={() => {}}
        facts={[{ term: 'Stage', value: 'Won' }]}
        navigator={{ position: { index: 1, total: 3 } }}
      />,
    );
    expect(html).toContain('mn-record-header');
    expect(html).toContain('2 of 3');
  });
});
