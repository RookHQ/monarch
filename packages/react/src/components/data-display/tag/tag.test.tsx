import { describe, expect, it, vi } from 'vitest';
import { createRef, useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Tag, TagGroup } from './index.js';

function Labels({ initial = ['Design', 'Research', 'Billing'] }: { initial?: string[] }) {
  const [tags, setTags] = useState(initial);
  return (
    <TagGroup label="Labels">
      {tags.map((t) => (
        <Tag key={t} onRemove={() => setTags((all) => all.filter((x) => x !== t))}>
          {t}
        </Tag>
      ))}
    </TagGroup>
  );
}

describe('Tag', () => {
  it('renders a static tag with tone and size, and forwards its ref', async () => {
    const ref = createRef<HTMLSpanElement>();
    render(
      <main>
        <Tag ref={ref} tone="info" size="sm">
          Design
        </Tag>
      </main>,
    );
    expect(ref.current).toHaveClass('mn-tag');
    expect(ref.current).toHaveAttribute('data-tone', 'info');
    expect(ref.current).toHaveAttribute('data-size', 'sm');
    expect(screen.queryByRole('button')).toBeNull();
    await expectNoA11yViolations();
  });

  it('is removable with a named button and with Backspace or Delete', async () => {
    const onRemove = vi.fn();
    render(
      <main>
        <Tag onRemove={onRemove}>Design</Tag>
      </main>,
    );
    const user = userEvent.setup();
    const remove = screen.getByRole('button', { name: 'Remove Design' });
    await user.click(remove);
    expect(onRemove).toHaveBeenCalledTimes(1);
    await user.keyboard('{Backspace}');
    expect(onRemove).toHaveBeenCalledTimes(2);
    await user.keyboard('{Delete}');
    expect(onRemove).toHaveBeenCalledTimes(3);
    await expectNoA11yViolations();
  });

  it('is a toggle button with aria-pressed when selectable', async () => {
    const onSelectedChange = vi.fn();
    render(
      <main>
        <Tag defaultSelected={false} onSelectedChange={onSelectedChange} data-testid="tag">
          Open
        </Tag>
      </main>,
    );
    const user = userEvent.setup();
    const toggle = screen.getByRole('button', { name: 'Open' });
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    await user.tab();
    await user.keyboard('{Enter}');
    expect(toggle).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByTestId('tag')).toHaveAttribute('data-selected');
    expect(toggle.querySelector('.mn-tag__check')).toHaveAttribute('aria-hidden', 'true');
    expect(onSelectedChange).toHaveBeenLastCalledWith(true);
    await user.keyboard(' ');
    expect(toggle).toHaveAttribute('aria-pressed', 'false');
    await expectNoA11yViolations();
  });

  it('can be selectable and removable at once', async () => {
    const onRemove = vi.fn();
    render(
      <Tag selected onSelectedChange={() => {}} onRemove={onRemove}>
        Assigned to me
      </Tag>,
    );
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Assigned to me' })).toHaveFocus();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Remove Assigned to me' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onRemove).toHaveBeenCalledOnce();
  });

  it('does nothing when disabled', async () => {
    const onRemove = vi.fn();
    const onSelectedChange = vi.fn();
    render(
      <Tag disabled onRemove={onRemove} onSelectedChange={onSelectedChange}>
        Archived
      </Tag>,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Archived' }));
    await user.click(screen.getByRole('button', { name: 'Remove Archived' }));
    expect(onRemove).not.toHaveBeenCalled();
    expect(onSelectedChange).not.toHaveBeenCalled();
  });

  it('translates the remove label and handles non-text labels', () => {
    render(
      <>
        <Tag onRemove={() => {}} messages={{ remove: (l) => `Quitar ${l}` }}>
          Diseño
        </Tag>
        <Tag onRemove={() => {}}>
          <strong>Bold</strong>
        </Tag>
      </>,
    );
    expect(screen.getByRole('button', { name: 'Quitar Diseño' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Remove tag' })).toBeInTheDocument();
  });
});

describe('TagGroup', () => {
  it('is a named group and moves focus to the next tag after a removal', async () => {
    render(<Labels />);
    const user = userEvent.setup();
    expect(screen.getByRole('group', { name: 'Labels' })).toHaveClass('mn-tag-group');
    await user.click(screen.getByRole('button', { name: 'Remove Design' }));
    expect(screen.getByRole('button', { name: 'Remove Research' })).toHaveFocus();
    await user.keyboard('{Delete}');
    expect(screen.getByRole('button', { name: 'Remove Billing' })).toHaveFocus();
    await user.keyboard('{Backspace}');
    expect(screen.getByRole('group', { name: 'Labels' })).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('moves focus to the previous tag when the last one is removed', async () => {
    render(<Labels initial={['One', 'Two']} />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Remove Two' }));
    expect(screen.getByRole('button', { name: 'Remove One' })).toHaveFocus();
  });
});

describe('Tag on the server', () => {
  it('renders', () => {
    const html = renderToString(
      <TagGroup label="Filters">
        <Tag defaultSelected onRemove={() => {}}>
          Open
        </Tag>
      </TagGroup>,
    );
    expect(html).toContain('mn-tag');
    expect(html).toContain('aria-pressed="true"');
  });
});
