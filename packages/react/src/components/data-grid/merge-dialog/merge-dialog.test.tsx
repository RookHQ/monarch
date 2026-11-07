import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { MergeDialog, defaultMergeChoice, type MergeRecord } from './index.js';

const RECORDS: MergeRecord[] = [
  {
    id: 'a',
    title: 'Example Ltd',
    description: 'Created 2024',
    values: { name: 'Example Ltd', email: 'hello@example.com', phone: '', city: 'Leeds' },
  },
  {
    id: 'b',
    title: 'Example Limited',
    values: {
      name: 'Example Limited',
      email: 'hello@example.com',
      phone: '+44 20 0000 0000',
      city: 'York',
    },
  },
];
const FIELDS = [
  { id: 'name', label: 'Name' },
  { id: 'email', label: 'Email' },
  { id: 'phone', label: 'Phone' },
  { id: 'city', label: 'City' },
];

function Harness(props: { onMerge: (s: unknown) => unknown; blockers?: string[] }) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>
        Merge
      </button>
      <MergeDialog
        open={open}
        onOpenChange={setOpen}
        records={RECORDS}
        fields={FIELDS}
        consequences={['3 invoices move to the kept record.']}
        {...props}
      />
    </>
  );
}

describe('MergeDialog', () => {
  it('lets people pick the record and the value to keep per field', async () => {
    const user = userEvent.setup();
    const onMerge = vi.fn();
    render(<Harness onMerge={onMerge} />);
    await user.click(screen.getByRole('button', { name: 'Merge' }));
    const dialog = await screen.findByRole('alertdialog', { name: 'Merge records' });
    await waitFor(() =>
      expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus(),
    );
    const keep = within(dialog).getByRole('group', { name: 'Record to keep' });
    expect(within(keep).getByRole('radio', { name: 'Example Ltd' })).toBeChecked();
    expect(within(keep).getByRole('radio', { name: 'Example Ltd' })).toHaveAccessibleDescription(
      'Created 2024',
    );
    // Email is the same everywhere: listed once, no choice.
    expect(within(dialog).queryByRole('group', { name: 'Email' })).toBeNull();
    expect(within(dialog).getByText('Same in every record')).toBeInTheDocument();
    // Phone is empty on the kept record, so the other value is chosen.
    expect(
      within(dialog).getByRole('radio', { name: '+44 20 0000 0000 from Example Limited' }),
    ).toBeChecked();
    const city = within(dialog).getByRole('group', { name: 'City' });
    expect(within(city).getByRole('radio', { name: 'Leeds from Example Ltd' })).toBeChecked();
    within(city).getByRole('radio', { name: 'Leeds from Example Ltd' }).focus();
    await user.keyboard('{ArrowRight}');
    expect(within(city).getByRole('radio', { name: 'York from Example Limited' })).toBeChecked();
    expect(within(dialog).getByText('3 invoices move to the kept record.')).toBeInTheDocument();
    await expectNoA11yViolations(dialog);
    await user.click(within(dialog).getByRole('button', { name: 'Merge 2 records' }));
    expect(onMerge).toHaveBeenCalledWith({
      primaryId: 'a',
      mergedIds: ['b'],
      fields: { name: 'a', email: 'a', phone: 'b', city: 'b' },
    });
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull());
    await waitFor(() => expect(screen.getByRole('button', { name: 'Merge' })).toHaveFocus());
  });

  it('changes defaults with the kept record', async () => {
    const user = userEvent.setup();
    const onMerge = vi.fn();
    render(<Harness onMerge={onMerge} />);
    await user.click(screen.getByRole('button', { name: 'Merge' }));
    const dialog = await screen.findByRole('alertdialog');
    await user.click(within(dialog).getByRole('radio', { name: 'Example Limited' }));
    expect(within(dialog).getByRole('radio', { name: 'York from Example Limited' })).toBeChecked();
    await user.click(within(dialog).getByRole('button', { name: 'Merge 2 records' }));
    expect(onMerge).toHaveBeenCalledWith(
      expect.objectContaining({ primaryId: 'b', mergedIds: ['a'] }),
    );
  });

  it('stays open and busy while merging, then shows a failure', async () => {
    const user = userEvent.setup();
    let reject: (e: Error) => void = () => {};
    const onMerge = vi.fn(() => new Promise((_, r) => (reject = r)));
    render(<Harness onMerge={onMerge} />);
    await user.click(screen.getByRole('button', { name: 'Merge' }));
    const dialog = await screen.findByRole('alertdialog');
    await user.click(within(dialog).getByRole('button', { name: 'Merge 2 records' }));
    expect(dialog).toHaveAttribute('aria-busy', 'true');
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toBeDisabled();
    await user.keyboard('{Escape}');
    expect(screen.getByRole('alertdialog')).toBeInTheDocument();
    await act(async () => reject(new Error('Conflict: record changed')));
    expect(within(dialog).getByRole('alert')).toHaveTextContent('Conflict: record changed');
    expect(dialog).not.toHaveAttribute('aria-busy');
  });

  it('blocks the merge while there are blockers', async () => {
    const user = userEvent.setup();
    render(<Harness onMerge={vi.fn()} blockers={['One record has an open invoice run.']} />);
    await user.click(screen.getByRole('button', { name: 'Merge' }));
    const dialog = await screen.findByRole('alertdialog');
    expect(within(dialog).getByText('One record has an open invoice run.')).toBeInTheDocument();
    expect(within(dialog).getByRole('button', { name: 'Merge 2 records' })).toBeDisabled();
  });

  it('opens from a trigger', async () => {
    const user = userEvent.setup();
    render(
      <MergeDialog
        trigger={<button type="button">Merge duplicates</button>}
        records={RECORDS}
        fields={FIELDS}
        onMerge={() => {}}
        messages={{ title: 'Merge clients', confirm: (n) => `Merge ${n} clients` }}
      />,
    );
    await user.click(screen.getByRole('button', { name: 'Merge duplicates' }));
    const dialog = await screen.findByRole('alertdialog', { name: 'Merge clients' });
    expect(within(dialog).getByRole('button', { name: 'Merge 2 clients' })).toBeInTheDocument();
    await user.keyboard('{Escape}');
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Merge duplicates' })).toHaveFocus(),
    );
  });

  it('picks defaults', () => {
    expect(defaultMergeChoice('phone', RECORDS, 'a')).toBe('b');
    expect(defaultMergeChoice('city', RECORDS, 'a')).toBe('a');
    expect(defaultMergeChoice('missing', RECORDS, 'a')).toBe('a');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <MergeDialog defaultOpen records={RECORDS} fields={FIELDS} onMerge={() => {}} />,
    );
    expect(typeof html).toBe('string');
  });
});
