import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { AiDraftQueue, type AiDraft, type AiDraftQueueProps } from './index.js';

const DRAFTS: AiDraft[] = [
  { id: 'a', title: 'Reply to Alex', body: 'Thanks, Thursday works.', status: 'draft' },
  {
    id: 'b',
    title: 'Reply to Sam',
    body: 'The report is attached.',
    context: 'Can you send the report?',
    status: 'draft',
  },
  { id: 'c', title: 'Reply to Kim', body: 'Noted.', status: 'draft' },
];

function Queue(props: Partial<AiDraftQueueProps> & { initial?: AiDraft[] }) {
  const { initial = DRAFTS, ...rest } = props;
  const [drafts, setDrafts] = useState(initial);
  const set = (id: string, patch: Partial<AiDraft>) =>
    setDrafts((ds) => ds.map((d) => (d.id === id ? { ...d, ...patch } : d)));
  return (
    <AiDraftQueue
      drafts={drafts}
      onEdit={(id, body) => set(id, { body })}
      onRegenerate={(id) => set(id, { status: 'regenerating' })}
      onApprove={(id) => set(id, { status: 'approved' })}
      onSend={(id) => set(id, { status: 'sent' })}
      onSkip={(id) => set(id, { status: 'skipped' })}
      {...rest}
    />
  );
}

const card = (title: string) => screen.getByRole('article', { name: title });

describe('AiDraftQueue', () => {
  it('lists drafts with status, context and progress', async () => {
    render(<Queue title="Inbox drafts" />);
    const queue = screen.getByRole('region', { name: 'Inbox drafts' });
    expect(queue).toHaveClass('mn-ai-draft-queue');
    expect(screen.getByRole('heading', { level: 2, name: 'Inbox drafts' })).toBeInTheDocument();
    expect(screen.getAllByRole('heading', { level: 3 })).toHaveLength(3);
    expect(screen.getByText('0 of 3 reviewed')).toBeInTheDocument();
    const sam = card('Reply to Sam');
    expect(within(sam).getByText('Needs review')).toBeInTheDocument();
    expect(within(sam).getByText('Can you send the report?')).toBeInTheDocument();
    expect(within(sam).getByText('AI draft:')).toHaveClass('mn-visually-hidden');
    await expectNoA11yViolations();
  });

  it('approves, moves focus to the next draft and announces the change', async () => {
    const user = userEvent.setup();
    render(<Queue />);
    await user.click(within(card('Reply to Alex')).getByRole('button', { name: 'Approve' }));
    expect(card('Reply to Alex')).toHaveAttribute('data-status', 'approved');
    expect(card('Reply to Sam')).toHaveFocus();
    expect(screen.getByText('1 of 3 reviewed')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Approved: Reply to Alex');
    // Approved drafts can be sent.
    await user.click(within(card('Reply to Alex')).getByRole('button', { name: 'Send' }));
    expect(within(card('Reply to Alex')).getByText('Sent')).toBeInTheDocument();
    expect(within(card('Reply to Alex')).queryByRole('button')).not.toBeInTheDocument();
  });

  it('skips and wraps to the first draft still needing review', async () => {
    const user = userEvent.setup();
    render(<Queue />);
    await user.click(within(card('Reply to Kim')).getByRole('button', { name: 'Skip' }));
    expect(card('Reply to Kim')).toHaveAttribute('data-status', 'skipped');
    expect(card('Reply to Alex')).toHaveFocus();
  });

  it('edits with save and cancel, from the keyboard too', async () => {
    const user = userEvent.setup();
    render(<Queue />);
    await user.click(within(card('Reply to Alex')).getByRole('button', { name: 'Edit' }));
    const box = screen.getByRole('textbox', { name: 'Edit draft: Reply to Alex' });
    expect(box).toHaveFocus();
    await user.clear(box);
    await user.type(box, 'Thursday at 10.');
    await user.keyboard('{Control>}{Enter}{/Control}');
    expect(within(card('Reply to Alex')).getByText('Thursday at 10.')).toBeInTheDocument();
    expect(card('Reply to Alex')).toHaveFocus();

    await user.click(within(card('Reply to Alex')).getByRole('button', { name: 'Edit' }));
    await user.type(screen.getByRole('textbox'), ' Extra');
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    expect(within(card('Reply to Alex')).getByText('Thursday at 10.')).toBeInTheDocument();
  });

  it('shows the writing state while regenerating', async () => {
    const user = userEvent.setup();
    const onRegenerate = vi.fn();
    render(<Queue onRegenerate={onRegenerate} />);
    await user.click(within(card('Reply to Sam')).getByRole('button', { name: 'Regenerate' }));
    expect(onRegenerate).toHaveBeenCalledWith('b');
  });

  it('marks a regenerating draft busy with no actions', async () => {
    render(<Queue initial={[{ ...DRAFTS[0]!, status: 'regenerating' }]} />);
    const draft = card('Reply to Alex');
    expect(draft).toHaveAttribute('aria-busy', 'true');
    expect(within(draft).getAllByText('AI is still writing…').length).toBeGreaterThan(0);
    expect(within(draft).queryByRole('button')).not.toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('J and K move between drafts only when opted in, and never while typing', async () => {
    const user = userEvent.setup();
    const { unmount } = render(<Queue keyboardShortcuts />);
    expect(screen.getByText('to move between drafts')).toBeInTheDocument();
    card('Reply to Alex').focus();
    await user.keyboard('j');
    expect(card('Reply to Sam')).toHaveFocus();
    await user.keyboard('j');
    await user.keyboard('j');
    expect(card('Reply to Kim')).toHaveFocus();
    await user.keyboard('k');
    expect(card('Reply to Sam')).toHaveFocus();
    await user.click(within(card('Reply to Sam')).getByRole('button', { name: 'Edit' }));
    await user.keyboard('j');
    expect(screen.getByRole('textbox')).toHaveFocus();
    expect((screen.getByRole('textbox') as HTMLTextAreaElement).value).toHaveLength(
      'The report is attached.'.length + 1,
    );
    unmount();

    render(<Queue />);
    card('Reply to Alex').focus();
    await user.keyboard('j');
    expect(card('Reply to Alex')).toHaveFocus();
  });

  it('shows the empty state and accepts strings', () => {
    render(
      <AiDraftQueue
        drafts={[]}
        onEdit={() => {}}
        onRegenerate={() => {}}
        onApprove={() => {}}
        onSkip={() => {}}
        messages={{ empty: 'Nichts zu prüfen.', label: 'Entwürfe' }}
      />,
    );
    expect(screen.getByRole('region', { name: 'Entwürfe' })).toHaveTextContent('Nichts zu prüfen.');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiDraftQueue
        drafts={DRAFTS}
        onEdit={() => {}}
        onRegenerate={() => {}}
        onApprove={() => {}}
        onSkip={() => {}}
      />,
    );
    expect(html).toContain('mn-ai-draft-queue');
    expect(html).toContain('Reply to Sam');
  });
});
