import { describe, expect, it, vi } from 'vitest';
import { createRef } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Button } from '../../../components/actions/button/index.js';
import { ConfirmDialog } from '../../../components/overlays/confirm-dialog/index.js';
import { DangerZone, DangerZoneItem } from './index.js';

function Zone(props: { onConfirm?: () => void; onTransfer?: () => void }) {
  return (
    <DangerZone description="These actions cannot be undone.">
      <DangerZoneItem
        title="Transfer ownership"
        description="Another member becomes the owner. You stay an admin."
        action={<Button onClick={props.onTransfer}>Transfer ownership</Button>}
      />
      <DangerZoneItem
        title="Delete this workspace"
        description="Projects, tasks and files are removed for everyone."
        action={
          <ConfirmDialog
            trigger={<Button variant="danger">Delete workspace</Button>}
            tone="danger"
            title="Delete this workspace?"
            confirmLabel="Delete workspace"
            onConfirm={props.onConfirm ?? (() => {})}
          />
        }
      />
    </DangerZone>
  );
}

describe('DangerZone', () => {
  it('is a region named by its default heading, forwarding ref and props', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <DangerZone ref={ref} data-testid="root">
        <DangerZoneItem title="Leave workspace" action={<Button variant="danger">Leave</Button>} />
      </DangerZone>,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-danger-zone');
    expect(ref.current).toBe(root);
    expect(screen.getByRole('region', { name: 'Danger zone' })).toBe(root);
    expect(screen.getByRole('heading', { level: 2, name: 'Danger zone' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('takes a custom heading and level; item titles are one level down', () => {
    render(
      <DangerZone heading="Irreversible actions" headingLevel="h3">
        <DangerZoneItem title="Delete account" action={<Button variant="danger">Delete</Button>} />
      </DangerZone>,
    );
    expect(screen.getByRole('heading', { level: 3, name: 'Irreversible actions' })).toBeVisible();
    expect(screen.getByRole('heading', { level: 4, name: 'Delete account' })).toBeVisible();
  });

  it('names each item group by its title and describes it by its description', async () => {
    render(<Zone />);
    const group = screen.getByRole('group', { name: 'Delete this workspace' });
    expect(group).toHaveAccessibleDescription(
      'Projects, tasks and files are removed for everyone.',
    );
    expect(screen.getByRole('heading', { level: 3, name: 'Transfer ownership' })).toBeVisible();
    await expectNoA11yViolations();
  });

  it('moves through the actions with Tab and opens a ConfirmDialog with focus return', async () => {
    const onTransfer = vi.fn();
    const onConfirm = vi.fn();
    render(<Zone onTransfer={onTransfer} onConfirm={onConfirm} />);
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Transfer ownership' })).toHaveFocus();
    await user.keyboard('{Enter}');
    expect(onTransfer).toHaveBeenCalledOnce();
    await user.tab();
    const trigger = screen.getByRole('button', { name: 'Delete workspace' });
    expect(trigger).toHaveFocus();
    await user.keyboard('{Enter}');
    const dialog = await screen.findByRole('alertdialog', { name: 'Delete this workspace?' });
    expect(dialog).toBeVisible();
    await user.keyboard('{Escape}');
    expect(onConfirm).not.toHaveBeenCalled();
    expect(trigger).toHaveFocus();
  });

  it('shows a disabled reason as text and adds it to the item description', async () => {
    render(
      <DangerZone>
        <DangerZoneItem
          data-testid="item"
          title="Leave workspace"
          description="You lose access to its projects."
          disabledReason="You are the only owner. Transfer ownership first."
          action={
            <Button variant="danger" disabled>
              Leave workspace
            </Button>
          }
        />
      </DangerZone>,
    );
    const item = screen.getByTestId('item');
    expect(item).toHaveAttribute('data-disabled', 'true');
    expect(item).toHaveTextContent('You are the only owner. Transfer ownership first.');
    expect(screen.getByRole('group', { name: 'Leave workspace' })).toHaveAccessibleDescription(
      'You lose access to its projects. You are the only owner. Transfer ownership first.',
    );
    expect(screen.getByRole('button', { name: 'Leave workspace' })).toBeDisabled();
    await expectNoA11yViolations();
  });

  it('uses a paragraph title outside a DangerZone', () => {
    render(<DangerZoneItem title="Delete account" action={<Button>Delete</Button>} />);
    expect(screen.queryByRole('heading')).toBeNull();
    expect(screen.getByRole('group', { name: 'Delete account' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<Zone />);
    expect(html).toContain('mn-danger-zone');
    expect(html).toContain('Danger zone');
    expect(html).toContain('mn-danger-zone-item');
  });
});
