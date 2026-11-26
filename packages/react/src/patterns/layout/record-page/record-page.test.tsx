import { createRef } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { RecordPage, type RecordPageTab } from './index.js';

const tabs: RecordPageTab[] = [
  { value: 'overview', label: 'Overview', content: <p>Overview body</p> },
  { value: 'notes', label: 'Notes', count: 3, content: <p>Notes body</p> },
  { value: 'files', label: 'Files', content: <p>Files body</p> },
];

describe('RecordPage', () => {
  it('renders the record header, named tabs, main panel, aside and activity', async () => {
    render(
      <RecordPage
        data-testid="root"
        title="Record A"
        headerProps={{ subtitle: 'REC-001' }}
        tabs={tabs}
        aside={<p>Owner: Person A</p>}
        activity={<p>Created</p>}
      />,
    );
    expect(screen.getByTestId('root')).toHaveClass('mn-record-page');
    expect(screen.getByRole('heading', { level: 1, name: 'Record A' })).toBeVisible();
    expect(screen.getByText('REC-001')).toBeVisible();
    expect(screen.getByRole('tablist', { name: 'Record sections' })).toBeVisible();
    expect(screen.getByRole('tab', { name: 'Overview' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tabpanel', { name: 'Overview' })).toHaveTextContent('Overview body');
    const aside = screen.getByRole('complementary', { name: 'Record details' });
    expect(aside).toHaveTextContent('Owner: Person A');
    expect(screen.getByRole('region', { name: 'Activity' })).toHaveTextContent('Created');
    await expectNoA11yViolations();
  });

  it('switches tabs with the arrow keys and calls onTabChange', async () => {
    const user = userEvent.setup();
    const onTabChange = vi.fn();
    render(<RecordPage title="Record A" tabs={tabs} onTabChange={onTabChange} />);
    await user.tab();
    expect(screen.getByRole('tab', { name: 'Overview' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    const notes = screen.getByRole('tab', { name: 'Notes 3' });
    expect(notes).toHaveFocus();
    expect(notes).toHaveAttribute('aria-selected', 'true');
    expect(onTabChange).toHaveBeenCalledWith('notes');
    expect(screen.getByRole('tabpanel')).toHaveTextContent('Notes body');
  });

  it('can be controlled and start on another tab', async () => {
    const user = userEvent.setup();
    const onTabChange = vi.fn();
    const { rerender } = render(
      <RecordPage title="Record A" tabs={tabs} tab="files" onTabChange={onTabChange} />,
    );
    expect(screen.getByRole('tabpanel')).toHaveTextContent('Files body');
    await user.click(screen.getByRole('tab', { name: 'Overview' }));
    expect(onTabChange).toHaveBeenCalledWith('overview');
    expect(screen.getByRole('tabpanel')).toHaveTextContent('Files body');
    rerender(<RecordPage title="Record A" tabs={tabs} tab="overview" />);
    expect(screen.getByRole('tabpanel')).toHaveTextContent('Overview body');
  });

  it('renders children without tabs, activity under main, and a custom header', async () => {
    render(
      <RecordPage
        title="unused"
        header={<h1>Custom header</h1>}
        activity={<p>Edited</p>}
        activityPlacement="main"
        activityTitle="History"
      >
        <p>Main body</p>
      </RecordPage>,
    );
    expect(screen.getByRole('heading', { name: 'Custom header' })).toBeVisible();
    expect(screen.queryByRole('tablist')).toBeNull();
    expect(screen.queryByRole('complementary')).toBeNull();
    expect(screen.getByRole('region', { name: 'History' })).toHaveTextContent('Edited');
    expect(screen.getByText('Main body')).toBeVisible();
    await expectNoA11yViolations();
  });

  it('translates labels, hides the activity heading and forwards the ref', () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <RecordPage
        ref={ref}
        title="Registro"
        tabs={tabs}
        aside={<p>Aside</p>}
        activity={<p>Log</p>}
        activityTitle={null}
        asideWidth="lg"
        messages={{ tabsLabel: 'Secciones', asideLabel: 'Detalles', activity: 'Actividad' }}
      />,
    );
    expect(ref.current).toHaveAttribute('data-aside-width', 'lg');
    expect(screen.getByRole('tablist', { name: 'Secciones' })).toBeVisible();
    expect(screen.getByRole('complementary', { name: 'Detalles' })).toBeVisible();
    expect(screen.getByRole('region', { name: 'Actividad' })).toHaveTextContent('Log');
    expect(screen.queryByRole('heading', { name: 'Actividad' })).toBeNull();
  });

  it('renders on the server', () => {
    const html = renderToString(<RecordPage title="Record A" tabs={tabs} aside="Aside" />);
    expect(html).toContain('mn-record-page');
    expect(html).toContain('Overview body');
  });
});
