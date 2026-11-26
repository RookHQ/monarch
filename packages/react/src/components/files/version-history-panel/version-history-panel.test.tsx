import { describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  collapseVersionDiff,
  diffVersionLines,
  VersionHistoryPanel,
  versionDiffStats,
  type VersionHistoryItem,
} from './index.js';

const NOW = Date.now();
const versions: VersionHistoryItem[] = [
  { id: 'v3', createdAt: NOW - 60_000, author: { name: 'Robin Park' }, note: 'Ready for review' },
  { id: 'v2', createdAt: NOW - 3_600_000, author: { name: 'Alex Chen' }, automatic: true },
  { id: 'v1', createdAt: NOW - 86_400_000, author: { name: 'Robin Park' } },
];
const now = ['# Plan', 'Goals for the quarter', 'Ship the beta', 'Hire two people'];
const old = ['# Plan', 'Goals for the year', 'Ship the beta'];

describe('version diff helpers', () => {
  it('diffs, counts and collapses lines', () => {
    const diff = diffVersionLines(old, now);
    expect(diff).toEqual([
      { kind: 'same', text: '# Plan' },
      { kind: 'removed', text: 'Goals for the year' },
      { kind: 'added', text: 'Goals for the quarter' },
      { kind: 'same', text: 'Ship the beta' },
      { kind: 'added', text: 'Hire two people' },
    ]);
    expect(versionDiffStats(diff)).toEqual({ added: 2, removed: 1 });
    const many = diffVersionLines(
      ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'x'],
      ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'y'],
    );
    expect(collapseVersionDiff(many, 2)).toEqual([
      { kind: 'same', text: 'a' },
      { kind: 'same', text: 'b' },
      { kind: 'gap', count: 3 },
      { kind: 'same', text: 'f' },
      { kind: 'same', text: 'g' },
      { kind: 'removed', text: 'x' },
      { kind: 'added', text: 'y' },
    ]);
  });
});

describe('VersionHistoryPanel', () => {
  it('lists versions with author, note, auto and current markers', async () => {
    render(<VersionHistoryPanel versions={versions} currentLines={now} onClose={() => {}} />);
    const panel = screen.getByRole('complementary', { name: 'History' });
    const list = within(panel).getByRole('list', { name: 'Versions' });
    const rows = within(list).getAllByRole('button');
    expect(rows).toHaveLength(3);
    expect(rows[0]).toHaveTextContent('Current');
    expect(rows[0]).toHaveTextContent('Robin Park · Ready for review');
    expect(rows[1]).toHaveTextContent('Alex Chen · auto');
    expect(screen.getByRole('button', { name: 'Close history' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('opens a version, shows the diff and full text, and returns focus to its row', async () => {
    const onSelectedIdChange = vi.fn();
    const { rerender } = render(
      <VersionHistoryPanel
        versions={versions}
        currentLines={now}
        onSelectedIdChange={onSelectedIdChange}
      />,
    );
    const user = userEvent.setup();
    const row = screen.getAllByRole('button')[1]!;
    await user.click(row);
    expect(onSelectedIdChange).toHaveBeenCalledWith('v2');
    expect(screen.getByRole('status', { name: 'Loading' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Back to version list' })).toHaveFocus();

    rerender(
      <VersionHistoryPanel
        versions={versions}
        currentLines={now}
        selectedLines={old}
        onSelectedIdChange={onSelectedIdChange}
      />,
    );
    expect(screen.getByText('2 added,', { exact: false })).toBeInTheDocument();
    expect(screen.getByRole('deletion')).toHaveTextContent('Removed: Goals for the year');
    expect(screen.getAllByRole('insertion')[0]).toHaveTextContent('Added: Goals for the quarter');
    await expectNoA11yViolations();

    await user.click(screen.getByRole('tab', { name: 'Full text' }));
    expect(screen.getByRole('tabpanel')).toHaveTextContent('Goals for the year');

    await user.click(screen.getByRole('button', { name: 'Back to version list' }));
    expect(onSelectedIdChange).toHaveBeenLastCalledWith(null);
    await waitFor(() => expect(screen.getAllByRole('button')[1]).toHaveFocus());
  });

  it('says when a version matches the current content', () => {
    render(
      <VersionHistoryPanel
        versions={versions}
        selectedId="v3"
        currentLines={now}
        selectedLines={now}
      />,
    );
    expect(screen.getByText('This version is identical to the current one.')).toBeInTheDocument();
  });

  it('saves a named version with Enter and shows failures', async () => {
    const onSaveVersion = vi
      .fn<(note: string) => Promise<void>>()
      .mockResolvedValueOnce()
      .mockRejectedValueOnce(new Error('You are offline.'));
    render(
      <VersionHistoryPanel versions={versions} currentLines={now} onSaveVersion={onSaveVersion} />,
    );
    const user = userEvent.setup();
    const input = screen.getByRole('textbox', { name: 'Version name' });
    await user.type(input, '  Before launch  {Enter}');
    expect(onSaveVersion).toHaveBeenCalledWith('Before launch');
    await waitFor(() => expect(input).toHaveValue(''));
    expect(screen.getByText('Version saved')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Save version' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('You are offline.');
  });

  it('restores after confirmation, but not the current version', async () => {
    const onRestore = vi.fn(async () => {});
    const onSelectedIdChange = vi.fn();
    const { rerender } = render(
      <VersionHistoryPanel
        versions={versions}
        selectedId="v2"
        onSelectedIdChange={onSelectedIdChange}
        currentLines={now}
        selectedLines={old}
        onRestore={onRestore}
      />,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Restore this version' }));
    const dialog = await screen.findByRole('alertdialog', { name: 'Restore this version?' });
    expect(onRestore).not.toHaveBeenCalled();
    await user.click(within(dialog).getByRole('button', { name: 'Restore' }));
    expect(onRestore).toHaveBeenCalledWith('v2');
    await waitFor(() => expect(onSelectedIdChange).toHaveBeenCalledWith(null));

    rerender(
      <VersionHistoryPanel
        versions={versions}
        selectedId="v3"
        currentLines={now}
        selectedLines={now}
        onRestore={onRestore}
      />,
    );
    expect(screen.queryByRole('button', { name: 'Restore this version' })).not.toBeInTheDocument();
  });

  it('shows loading and empty states, and translates strings', () => {
    const { rerender } = render(<VersionHistoryPanel versions={[]} currentLines={now} loading />);
    expect(screen.getByRole('status', { name: 'Loading' })).toBeInTheDocument();
    rerender(
      <VersionHistoryPanel
        versions={[]}
        currentLines={now}
        messages={{ empty: 'Noch nichts.', title: 'Verlauf' }}
      />,
    );
    expect(screen.getByText('Noch nichts.')).toBeInTheDocument();
    expect(screen.getByRole('complementary', { name: 'Verlauf' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<VersionHistoryPanel versions={versions} currentLines={now} />);
    expect(html).toContain('mn-version-history-panel');
  });
});
