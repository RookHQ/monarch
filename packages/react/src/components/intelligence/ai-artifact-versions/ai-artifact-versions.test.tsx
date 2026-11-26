import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiArtifactDiff,
  AiArtifactVersions,
  diffLines,
  type AiArtifactVersionInfo,
} from './index.js';

const versions: AiArtifactVersionInfo[] = [
  { version: 3, createdAt: '2026-03-12T14:05:00Z', label: 'Shorter intro' },
  { version: 1, createdAt: '2026-03-12T13:00:00Z' },
  { version: 2, createdAt: '2026-03-12T13:30:00Z' },
];

function Controlled({ onCompare }: { onCompare?: (a: number, b: number) => void }) {
  const [value, setValue] = useState(2);
  return (
    <AiArtifactVersions
      versions={versions}
      value={value}
      onChange={setValue}
      onCompare={onCompare}
      messages={{ formatDate: () => 'date' }}
    />
  );
}

describe('AiArtifactVersions', () => {
  it('is a named group with position, previous / next and a select', async () => {
    render(<Controlled />);
    expect(screen.getByRole('group', { name: 'Versions' })).toHaveClass('mn-ai-artifact-versions');
    expect(screen.getByText('Version 2 of 3')).toBeInTheDocument();
    const select = screen.getByRole('combobox', { name: 'Choose a version' });
    expect(select).toHaveValue('2');
    expect(screen.getAllByRole('option').map((o) => o.textContent)).toEqual([
      'v1 · date',
      'v2 · date',
      'v3 · Shorter intro · date',
    ]);
    await expectNoA11yViolations();
  });

  it('steps with the buttons, disables at the ends and announces changes', async () => {
    const user = userEvent.setup();
    render(<Controlled />);
    const prev = screen.getByRole('button', { name: 'Previous version' });
    const next = screen.getByRole('button', { name: 'Next version' });
    expect(screen.getByRole('status')).toHaveTextContent('');
    await user.click(next);
    expect(screen.getByText('Version 3 of 3')).toBeInTheDocument();
    expect(next).toBeDisabled();
    expect(screen.getByRole('status')).toHaveTextContent('Showing version 3 of 3');
    await user.click(prev);
    await user.click(prev);
    expect(prev).toBeDisabled();
    expect(screen.getByRole('combobox')).toHaveValue('1');
  });

  it('jumps with the select by keyboard', async () => {
    const user = userEvent.setup();
    render(<Controlled />);
    await user.selectOptions(screen.getByRole('combobox'), '3');
    expect(screen.getByText('Version 3 of 3')).toBeInTheDocument();
  });

  it('compares with the previous version', async () => {
    const user = userEvent.setup();
    const onCompare = vi.fn();
    render(<Controlled onCompare={onCompare} />);
    await user.click(screen.getByRole('button', { name: 'Compare with previous' }));
    expect(onCompare).toHaveBeenCalledWith(1, 2);
  });
});

describe('diffLines', () => {
  it('marks kept, removed and added lines', () => {
    expect(diffLines('a\nb\nc', 'a\nB\nc\nd')).toBe(' a\n-b\n+B\n c\n+d');
    expect(diffLines('same', 'same')).toBe(' same');
  });
});

describe('AiArtifactDiff', () => {
  it('marks removed and added words with text as well as colour', async () => {
    render(<AiArtifactDiff before="The quick fox" after="The slow fox" from={1} to={2} />);
    const group = screen.getByRole('group', { name: 'Changes from version 1 to version 2' });
    expect(group.querySelector('del')).toHaveTextContent('Removed: quick');
    expect(group.querySelector('ins')).toHaveTextContent('Added: slow');
    await expectNoA11yViolations();
  });

  it('shows a line diff for code and a message when nothing changed', () => {
    const { rerender } = render(<AiArtifactDiff mode="lines" before={'a\nb'} after={'a\nc'} />);
    expect(screen.getByRole('group', { name: 'Changes' })).toHaveTextContent('b');
    expect(document.querySelector('.mn-ai-code-block')).not.toBeNull();
    rerender(<AiArtifactDiff before="x" after="x" />);
    expect(screen.getByText('No changes between these versions.')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(
      renderToString(<AiArtifactVersions versions={versions} value={1} onChange={() => {}} />),
    ).toContain('Version 1 of 3');
    expect(renderToString(<AiArtifactDiff before="a" after="b" />)).toContain('<ins');
  });
});
