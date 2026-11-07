import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { createRef, useState } from 'react';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  AiKnowledgeScope,
  aiKnowledgeScopeState,
  toggleAiKnowledgeSource,
  type AiKnowledgeSource,
} from './index.js';

const SOURCES: AiKnowledgeSource[] = [
  {
    id: 'handbook',
    label: 'Handbook',
    kind: 'space',
    description: '3 documents',
    children: [
      { id: 'policies', label: 'Policies', kind: 'doc' },
      { id: 'onboarding', label: 'Onboarding', kind: 'doc' },
      { id: 'travel', label: 'Travel', kind: 'doc' },
    ],
  },
  { id: 'calendar', label: 'Calendar', kind: 'app' },
];

function Controlled(props: { initial?: string[]; onChange?: (v: string[]) => void }) {
  const [value, setValue] = useState<string[]>(props.initial ?? []);
  return (
    <AiKnowledgeScope
      sources={SOURCES}
      value={value}
      onChange={(next) => {
        setValue(next);
        props.onChange?.(next);
      }}
    />
  );
}

describe('aiKnowledgeScopeState', () => {
  it('derives checked, indeterminate and inherited states', () => {
    const s = aiKnowledgeScopeState(SOURCES, ['policies']);
    expect(s.get('policies')).toBe('checked');
    expect(s.get('handbook')).toBe('indeterminate');
    expect(s.get('travel')).toBe('unchecked');
    const all = aiKnowledgeScopeState(SOURCES, ['handbook']);
    expect(all.get('travel')).toBe('checked');
    expect(all.get('handbook')).toBe('checked');
    const byChildren = aiKnowledgeScopeState(SOURCES, ['policies', 'onboarding', 'travel']);
    expect(byChildren.get('handbook')).toBe('checked');
  });
});

describe('toggleAiKnowledgeSource', () => {
  it('checks a group with its children and unchecks them again', () => {
    expect(toggleAiKnowledgeSource(SOURCES, [], 'handbook')).toEqual([
      'handbook',
      'policies',
      'onboarding',
      'travel',
    ]);
    expect(toggleAiKnowledgeSource(SOURCES, ['handbook'], 'handbook')).toEqual([]);
  });

  it('unchecks an indeterminate group and expands a group id when a child is removed', () => {
    expect(toggleAiKnowledgeSource(SOURCES, ['policies'], 'handbook')).toEqual([]);
    expect(toggleAiKnowledgeSource(SOURCES, ['handbook'], 'travel')).toEqual([
      'policies',
      'onboarding',
    ]);
  });

  it('promotes a group once all children are checked and keeps unknown ids', () => {
    expect(toggleAiKnowledgeSource(SOURCES, ['policies', 'onboarding', 'x'], 'travel')).toEqual([
      'handbook',
      'policies',
      'onboarding',
      'travel',
      'x',
    ]);
    expect(toggleAiKnowledgeSource(SOURCES, ['a'], 'missing')).toEqual(['a']);
  });
});

describe('AiKnowledgeScope', () => {
  it('renders a named tree with checked states, forwards props and the ref', async () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <AiKnowledgeScope
        ref={ref}
        data-testid="root"
        className="extra"
        sources={SOURCES}
        value={['policies']}
        onChange={() => {}}
        defaultExpanded={['handbook']}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-ai-knowledge-scope', 'extra');
    expect(ref.current).toBe(root);
    const tree = screen.getByRole('tree', { name: 'Knowledge sources' });
    expect(tree).toBeInTheDocument();
    const handbook = screen.getByRole('treeitem', { name: 'Handbook, Space' });
    expect(handbook).toHaveAttribute('aria-checked', 'mixed');
    expect(handbook).toHaveAttribute('aria-expanded', 'true');
    expect(handbook).toHaveAccessibleDescription('3 documents');
    expect(screen.getByRole('treeitem', { name: 'Policies, Document' })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    expect(screen.getByRole('treeitem', { name: 'Calendar, App' })).toHaveAttribute(
      'aria-checked',
      'false',
    );
    expect(screen.getByText('1 source selected')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('moves with arrows, Home and End and expands and collapses groups', async () => {
    const user = userEvent.setup();
    render(<Controlled />);
    await user.tab();
    const handbook = screen.getByRole('treeitem', { name: 'Handbook, Space' });
    expect(handbook).toHaveFocus();
    expect(handbook).toHaveAttribute('aria-expanded', 'false');
    await user.keyboard('{ArrowRight}');
    expect(handbook).toHaveAttribute('aria-expanded', 'true');
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('treeitem', { name: 'Policies, Document' })).toHaveFocus();
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('treeitem', { name: 'Onboarding, Document' })).toHaveFocus();
    await user.keyboard('{End}');
    expect(screen.getByRole('treeitem', { name: 'Calendar, App' })).toHaveFocus();
    await user.keyboard('{Home}');
    expect(handbook).toHaveFocus();
    await user.keyboard('{ArrowDown}{ArrowLeft}');
    expect(handbook).toHaveFocus();
    await user.keyboard('{ArrowLeft}');
    expect(handbook).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByRole('treeitem', { name: 'Policies, Document' })).toBeNull();
    // Only one tab stop.
    await user.tab();
    expect(document.body).toHaveFocus();
  });

  it('toggles with Space and Enter and reports tri-state values', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Controlled onChange={onChange} />);
    await user.tab();
    await user.keyboard(' ');
    expect(onChange).toHaveBeenLastCalledWith(['handbook', 'policies', 'onboarding', 'travel']);
    const handbook = screen.getByRole('treeitem', { name: 'Handbook, Space' });
    expect(handbook).toHaveAttribute('aria-checked', 'true');
    await user.keyboard('{ArrowRight}{ArrowDown}{Enter}');
    expect(onChange).toHaveBeenLastCalledWith(['onboarding', 'travel']);
    expect(handbook).toHaveAttribute('aria-checked', 'mixed');
    expect(screen.getByText('2 sources selected')).toBeInTheDocument();
  });

  it('toggles by clicking a row and expands from the chevron', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const { container } = render(<Controlled onChange={onChange} />);
    await user.click(screen.getByText('Calendar'));
    expect(onChange).toHaveBeenLastCalledWith(['calendar']);
    await user.click(container.querySelector('.mn-ai-knowledge-scope__toggle')!);
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('treeitem', { name: 'Handbook, Space' })).toHaveAttribute(
      'aria-expanded',
      'true',
    );
  });

  it('honours disabled, controlled expansion and localised strings', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const onExpandedChange = vi.fn();
    render(
      <AiKnowledgeScope
        sources={SOURCES}
        value={[]}
        onChange={onChange}
        disabled
        expanded={[]}
        onExpandedChange={onExpandedChange}
        messages={{ label: 'Grounding', kinds: { space: 'Area' }, summary: (n) => `${n} picked` }}
      />,
    );
    expect(screen.getByRole('tree', { name: 'Grounding' })).toBeInTheDocument();
    await user.tab();
    await user.keyboard(' {ArrowRight}');
    expect(onChange).not.toHaveBeenCalled();
    expect(onExpandedChange).toHaveBeenCalledWith(['handbook']);
    expect(screen.getByRole('treeitem', { name: 'Handbook, Area' })).toHaveAttribute(
      'aria-expanded',
      'false',
    );
    expect(screen.getByText('0 picked')).toBeInTheDocument();
  });

  it('shows an empty message without sources', () => {
    render(<AiKnowledgeScope sources={[]} value={[]} onChange={() => {}} />);
    expect(screen.getByText('No sources available')).toBeInTheDocument();
    expect(screen.queryByRole('tree')).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AiKnowledgeScope sources={SOURCES} value={['handbook']} onChange={() => {}} />,
    );
    expect(html).toContain('role="tree"');
    expect(html).toContain('aria-checked="true"');
  });
});
