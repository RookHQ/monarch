import { useState, type KeyboardEvent } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  findMentionQuery,
  formatMentionToken,
  MentionPicker,
  parseMentionTokens,
  useMentionTextarea,
  type MentionItem,
  type MentionSearch,
} from './index.js';

const PEOPLE: MentionItem[] = [
  { id: 'u1', label: 'Ana Lopez', description: 'ana@example.com' },
  { id: 'u2', label: 'Andre Kim', description: 'andre@example.com' },
  { id: 'u3', label: 'Annie Ross', disabled: true },
];
const CHANNELS: MentionItem[] = [{ id: 'c1', label: 'announcements', kind: 'channel' }];

const searchPeople = (text: string): MentionItem[] =>
  PEOPLE.filter((p) => p.label.toLowerCase().includes(text.toLowerCase()));

function Harness({
  search = searchPeople,
  triggers,
  onMention,
  onKeyDown,
}: {
  search?: MentionSearch;
  triggers?: string[];
  onMention?: (item: MentionItem) => void;
  onKeyDown?: (e: KeyboardEvent<HTMLTextAreaElement>) => void;
}) {
  const [value, setValue] = useState('');
  const mention = useMentionTextarea({
    value,
    onChange: setValue,
    search,
    debounceMs: 0,
    ...(triggers ? { triggers } : {}),
    ...(onMention ? { onMention } : {}),
    ...(onKeyDown ? { onKeyDown } : {}),
  });
  return (
    <>
      <label htmlFor="comment">Comment</label>
      <textarea id="comment" {...mention.textareaProps} />
      <MentionPicker {...mention.pickerProps} />
      <span data-testid="value">{value}</span>
    </>
  );
}

const field = () => screen.getByRole('textbox', { name: 'Comment' });
/** The floating panel (the status region repeats its message for screen readers). */
const panel = () => within(document.querySelector<HTMLElement>('.mn-mention-picker')!);

describe('MentionPicker + useMentionTextarea', () => {
  it('opens a listbox for @ and wires the textarea to it', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.type(field(), 'Hi @an');
    const listbox = await screen.findByRole('listbox', { name: 'Suggestions' });
    const options = within(listbox).getAllByRole('option');
    expect(options.map((o) => o.querySelector('.mn-mention-picker__label')?.textContent)).toEqual([
      'Ana Lopez',
      'Andre Kim',
      'Annie Ross',
    ]);
    expect(field()).toHaveAttribute('aria-autocomplete', 'list');
    expect(field()).toHaveAttribute('aria-controls', listbox.id);
    expect(field()).toHaveAttribute('aria-activedescendant', options[0]!.id);
    expect(options[0]).toHaveAttribute('aria-selected', 'true');
    expect(options[2]).toHaveAttribute('aria-disabled', 'true');
    expect(screen.getByRole('status')).toHaveTextContent('3 suggestions');
    expect(field()).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('arrow keys move the highlight, wrap and skip disabled options', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.type(field(), '@an');
    const options = await screen.findAllByRole('option');
    await user.keyboard('{ArrowDown}');
    expect(field()).toHaveAttribute('aria-activedescendant', options[1]!.id);
    await user.keyboard('{ArrowDown}');
    expect(field()).toHaveAttribute('aria-activedescendant', options[0]!.id);
    await user.keyboard('{ArrowUp}');
    expect(field()).toHaveAttribute('aria-activedescendant', options[1]!.id);
  });

  it('Enter inserts a token, closes the list and leaves the caret after it', async () => {
    const onMention = vi.fn();
    const user = userEvent.setup();
    render(<Harness onMention={onMention} />);
    await user.type(field(), 'Ping @ana');
    await screen.findByRole('listbox');
    await user.keyboard('{Enter}');
    expect(screen.getByTestId('value')).toHaveTextContent('Ping @[Ana Lopez](u1)');
    expect((field() as HTMLTextAreaElement).value).toBe('Ping @[Ana Lopez](u1) ');
    expect(screen.queryByRole('listbox')).toBeNull();
    expect(field()).not.toHaveAttribute('aria-controls');
    expect(onMention).toHaveBeenCalledWith(PEOPLE[0]);
    await waitFor(() => expect((field() as HTMLTextAreaElement).selectionStart).toBe(22));
    await user.keyboard('thanks');
    expect((field() as HTMLTextAreaElement).value).toBe('Ping @[Ana Lopez](u1) thanks');
  });

  it('Tab selects; clicking an option selects and keeps focus in the field', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.type(field(), '@and');
    await screen.findByRole('listbox');
    await user.keyboard('{Tab}');
    expect((field() as HTMLTextAreaElement).value).toBe('@[Andre Kim](u2) ');
    expect(field()).toHaveFocus();
    await user.type(field(), '@an');
    await user.click(await screen.findByRole('option', { name: /Andre Kim/ }));
    expect((field() as HTMLTextAreaElement).value).toBe('@[Andre Kim](u2) @[Andre Kim](u2) ');
    expect(field()).toHaveFocus();
  });

  it('a disabled option cannot be chosen', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.type(field(), '@annie');
    await user.click(await screen.findByRole('option', { name: /Annie Ross/ }));
    expect((field() as HTMLTextAreaElement).value).toBe('@annie');
  });

  it('Escape closes without reaching outer handlers; keys pass through when closed', async () => {
    const outer = vi.fn();
    const onKeyDown = vi.fn();
    const user = userEvent.setup();
    render(
      // A stand-in for a dialog that listens for Escape.
      // eslint-disable-next-line jsx-a11y/no-static-element-interactions
      <div onKeyDown={outer}>
        <Harness onKeyDown={onKeyDown} />
      </div>,
    );
    await user.type(field(), '@an');
    await screen.findByRole('listbox');
    outer.mockClear();
    onKeyDown.mockClear();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('listbox')).toBeNull();
    expect(outer).not.toHaveBeenCalled();
    expect(onKeyDown).not.toHaveBeenCalled();
    await user.keyboard('{Enter}');
    expect(onKeyDown).toHaveBeenCalledTimes(1);
    expect(outer).toHaveBeenCalled();
  });

  it('groups people and channels under labelled groups', async () => {
    const user = userEvent.setup();
    render(<Harness search={(text) => [...searchPeople(text), ...CHANNELS]} />);
    await user.type(field(), '@an');
    const people = await screen.findByRole('group', { name: 'People' });
    const channels = screen.getByRole('group', { name: 'Channels' });
    expect(within(people).getAllByRole('option')).toHaveLength(3);
    expect(within(channels).getByRole('option', { name: /announcements/ })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('supports # as a second trigger', async () => {
    const search = vi.fn<MentionSearch>((_text, { trigger }) =>
      trigger === '#' ? CHANNELS : PEOPLE,
    );
    const user = userEvent.setup();
    render(<Harness search={search} triggers={['@', '#']} />);
    await user.type(field(), 'See #ann');
    await user.click(await screen.findByRole('option', { name: /announcements/ }));
    expect((field() as HTMLTextAreaElement).value).toBe('See #[announcements](c1) ');
    expect(search).toHaveBeenLastCalledWith('ann', expect.objectContaining({ trigger: '#' }));
  });

  it('aborts the previous search as the query changes', async () => {
    const signals: AbortSignal[] = [];
    const search: MentionSearch = (text, { signal }) => {
      signals.push(signal);
      return new Promise((resolve) => setTimeout(() => resolve(searchPeople(text)), 20));
    };
    const user = userEvent.setup();
    render(<Harness search={search} />);
    await user.type(field(), '@ana');
    expect(await screen.findByRole('option', { name: /Ana Lopez/ })).toBeInTheDocument();
    expect(signals.length).toBeGreaterThan(1);
    expect(signals.slice(0, -1).every((s) => s.aborted)).toBe(true);
    expect(signals[signals.length - 1]!.aborted).toBe(false);
  });

  it('shows Searching…, then an error with Retry', async () => {
    // Each search waits until the test settles it, so the loading state is observable on a slow machine.
    const pending: Array<{
      text: string;
      resolve: (v: MentionItem[]) => void;
      reject: (e: Error) => void;
    }> = [];
    const search: MentionSearch = (text) =>
      new Promise((resolve, reject) => pending.push({ text, resolve, reject }));
    const user = userEvent.setup();
    render(<Harness search={search} />);
    await user.type(field(), '@an');
    await waitFor(() => expect(pending.length).toBeGreaterThan(0));
    expect(panel().getByText('Searching…')).toBeInTheDocument();
    act(() => pending.at(-1)!.reject(new Error('offline')));
    await waitFor(() =>
      expect(panel().getByText('Couldn’t load suggestions.')).toBeInTheDocument(),
    );
    expect(screen.getByRole('status')).toHaveTextContent('Couldn’t load suggestions.');
    const before = pending.length;
    await user.click(screen.getByRole('button', { name: 'Retry' }));
    await waitFor(() => expect(pending.length).toBe(before + 1));
    act(() => pending.at(-1)!.resolve(searchPeople(pending.at(-1)!.text)));
    expect(await screen.findByRole('listbox')).toBeInTheDocument();
  });

  it('shows No matches, and closes when a two-word query matches nothing', async () => {
    const user = userEvent.setup();
    render(<Harness />);
    await user.type(field(), '@zz');
    await waitFor(() => expect(panel().getByText('No matches')).toBeInTheDocument());
    expect(field()).not.toHaveAttribute('aria-controls');
    await user.type(field(), ' top');
    await waitFor(() => expect(document.querySelector('.mn-mention-picker')).toBeNull());
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
  });

  it('does not open inside a word such as an email address', async () => {
    const search = vi.fn(searchPeople);
    const user = userEvent.setup();
    render(<Harness search={search} />);
    await user.type(field(), 'mail ana@example');
    await new Promise((r) => setTimeout(r, 10));
    expect(search).not.toHaveBeenCalled();
    expect(screen.queryByRole('listbox')).toBeNull();
  });

  it('closes when the field loses focus', async () => {
    const user = userEvent.setup();
    render(
      <>
        <Harness />
        <button type="button">Elsewhere</button>
      </>,
    );
    await user.type(field(), '@an');
    await screen.findByRole('listbox');
    await user.click(screen.getByRole('button', { name: 'Elsewhere' }));
    expect(screen.queryByRole('listbox')).toBeNull();
  });

  it('renders closed on the server', () => {
    const html = renderToString(
      <MentionPicker
        id="m"
        open={false}
        anchor={null}
        items={[]}
        status="idle"
        activeId={null}
        onSelect={() => {}}
        onActiveChange={() => {}}
      />,
    );
    expect(html).toContain('role="status"');
    expect(html).not.toContain('listbox');
  });
});

describe('mention helpers', () => {
  it('findMentionQuery finds a trigger at a word start, up to two words', () => {
    expect(findMentionQuery('Hi @an', 6)).toEqual({ trigger: '@', text: 'an', start: 3, end: 6 });
    expect(findMentionQuery('@', 1)).toEqual({ trigger: '@', text: '', start: 0, end: 1 });
    expect(findMentionQuery('(@ana lo', 8)).toEqual({
      trigger: '@',
      text: 'ana lo',
      start: 1,
      end: 8,
    });
    expect(findMentionQuery('a@b', 3)).toBeNull();
    expect(findMentionQuery('@ana lopez x', 12)).toBeNull();
    expect(findMentionQuery('#gen', 4, ['@', '#'])?.trigger).toBe('#');
  });

  it('formats and parses tokens', () => {
    const token = formatMentionToken({ id: 'u1', label: 'Ana (Design) [EU]' });
    expect(token).toBe('@[Ana Design EU](u1)');
    expect(parseMentionTokens(`Hi ${token} and #[general](c9)!`)).toEqual([
      { type: 'text', text: 'Hi ' },
      { type: 'mention', trigger: '@', label: 'Ana Design EU', id: 'u1' },
      { type: 'text', text: ' and ' },
      { type: 'mention', trigger: '#', label: 'general', id: 'c9' },
      { type: 'text', text: '!' },
    ]);
    expect(parseMentionTokens('plain')).toEqual([{ type: 'text', text: 'plain' }]);
  });
});
