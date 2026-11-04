import { describe, expect, it, vi } from 'vitest';
import { forwardRef, useState, type AnchorHTMLAttributes } from 'react';
import { render, renderHook, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  Tabs,
  TabsContent,
  TabsFilter,
  TabsLink,
  TabsList,
  TabsNav,
  TabsTrigger,
  useTabsUrlBinding,
} from './index.js';

describe('Tabs', () => {
  it('moves between tabs with arrow keys and shows the matching panel', async () => {
    render(
      <Tabs defaultValue="board">
        <TabsList aria-label="Views">
          <TabsTrigger value="board">Board</TabsTrigger>
          <TabsTrigger value="list">List</TabsTrigger>
        </TabsList>
        <TabsContent value="board">Board view</TabsContent>
        <TabsContent value="list">List view</TabsContent>
      </Tabs>,
    );
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('tab', { name: 'Board' })).toHaveFocus();
    await user.keyboard('{ArrowRight}');
    const list = screen.getByRole('tab', { name: 'List' });
    expect(list).toHaveFocus();
    expect(list).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tabpanel')).toHaveTextContent('List view');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <Tabs defaultValue="a">
        <TabsList>
          <TabsTrigger value="a">A</TabsTrigger>
        </TabsList>
        <TabsContent value="a">Panel</TabsContent>
      </Tabs>,
    );
    expect(html).toContain('mn-tabs__list');
  });
});

describe('TabsTrigger count and TabsList scrollable', () => {
  it('shows a count in the trigger and its name', async () => {
    render(
      <Tabs defaultValue="open">
        <TabsList aria-label="Tickets" scrollable>
          <TabsTrigger value="open" count={12} countTone="danger">
            Open
          </TabsTrigger>
          <TabsTrigger value="closed">Closed</TabsTrigger>
        </TabsList>
        <TabsContent value="open">Open tickets</TabsContent>
        <TabsContent value="closed">Closed tickets</TabsContent>
      </Tabs>,
    );
    const open = screen.getByRole('tab', { name: 'Open 12' });
    expect(open.querySelector('.mn-tabs__count')).toHaveAttribute('data-tone', 'danger');
    expect(screen.getByRole('tab', { name: 'Closed' }).querySelector('.mn-tabs__count')).toBeNull();
    expect(screen.getByRole('tablist')).toHaveAttribute('data-scrollable');
    await expectNoA11yViolations();
  });

  it('keeps asChild working when there is no count', () => {
    render(
      <Tabs defaultValue="a">
        <TabsList aria-label="Views">
          <TabsTrigger value="a" asChild>
            <button type="button" data-custom="">
              A
            </button>
          </TabsTrigger>
        </TabsList>
        <TabsContent value="a">Panel</TabsContent>
      </Tabs>,
    );
    expect(screen.getByRole('tab', { name: 'A' })).toHaveAttribute('data-custom');
  });
});

const RouterLink = forwardRef<HTMLAnchorElement, AnchorHTMLAttributes<HTMLAnchorElement>>(
  function RouterLink({ children, ...props }, ref) {
    return (
      <a ref={ref} data-router="" {...props}>
        {children}
      </a>
    );
  },
);

describe('TabsNav / TabsLink', () => {
  it('marks the current page with aria-current and uses link semantics', async () => {
    render(
      <TabsNav aria-label="Project sections">
        <TabsLink href="/overview" active>
          Overview
        </TabsLink>
        <TabsLink href="/issues" count={4}>
          Issues
        </TabsLink>
        <TabsLink asChild>
          <RouterLink href="/settings">Settings</RouterLink>
        </TabsLink>
      </TabsNav>,
    );
    expect(screen.getByRole('navigation', { name: 'Project sections' })).toBeInTheDocument();
    const overview = screen.getByRole('link', { name: 'Overview' });
    expect(overview).toHaveAttribute('aria-current', 'page');
    expect(overview).toHaveAttribute('data-state', 'active');
    const issues = screen.getByRole('link', { name: 'Issues 4' });
    expect(issues).not.toHaveAttribute('aria-current');
    const settings = screen.getByRole('link', { name: 'Settings' });
    expect(settings).toHaveAttribute('data-router');
    expect(settings).toHaveClass('mn-tabs__trigger');
    expect(screen.queryByRole('tab')).toBeNull();
    const user = userEvent.setup();
    await user.tab();
    expect(overview).toHaveFocus();
    await user.tab();
    expect(issues).toHaveFocus();
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <TabsNav aria-label="Sections">
        <TabsLink href="/a" active>
          A
        </TabsLink>
      </TabsNav>,
    );
    expect(html).toContain('aria-current="page"');
  });
});

const STATUS = [
  { value: 'all', label: 'All', count: 24 },
  { value: 'open', label: 'Open', count: 6 },
  { value: 'pending', label: 'Pending', disabled: true },
  { value: 'closed', label: 'Closed', count: 18 },
];

describe('TabsFilter', () => {
  it('uncontrolled: roving focus, arrow keys skip disabled, Home/End, selection follows focus', async () => {
    const onValueChange = vi.fn();
    render(<TabsFilter aria-label="Status" options={STATUS} onValueChange={onValueChange} />);
    const user = userEvent.setup();
    await user.tab();
    const all = screen.getByRole('tab', { name: 'All 24' });
    expect(all).toHaveFocus();
    expect(all).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: 'Open 6' })).toHaveAttribute('tabindex', '-1');
    await user.keyboard('{ArrowRight}{ArrowRight}');
    const closed = screen.getByRole('tab', { name: 'Closed 18' });
    expect(closed).toHaveFocus();
    expect(closed).toHaveAttribute('aria-selected', 'true');
    await user.keyboard('{ArrowRight}');
    expect(all).toHaveFocus();
    await user.keyboard('{End}');
    expect(closed).toHaveFocus();
    await user.keyboard('{Home}');
    expect(all).toHaveFocus();
    expect(onValueChange.mock.calls.map((c) => c[0])).toEqual([
      'open',
      'closed',
      'all',
      'closed',
      'all',
    ]);
    // No panel: tabs do not point at one.
    expect(all).not.toHaveAttribute('aria-controls');
    expect(screen.queryByRole('tabpanel')).toBeNull();
    await expectNoA11yViolations();
  });

  it('controlled with a shared panel, manual activation', async () => {
    function Controlled() {
      const [v, setV] = useState('open');
      return (
        <TabsFilter
          aria-label="Status"
          options={STATUS}
          value={v}
          onValueChange={setV}
          activationMode="manual"
          variant="pill"
        >
          Showing {v}
        </TabsFilter>
      );
    }
    render(<Controlled />);
    const panel = screen.getByRole('tabpanel');
    expect(panel).toHaveTextContent('Showing open');
    expect(panel).toHaveAccessibleName('Open 6');
    const open = screen.getByRole('tab', { name: 'Open 6' });
    expect(open).toHaveAttribute('aria-controls', panel.id);
    const user = userEvent.setup();
    await user.click(open);
    await user.keyboard('{ArrowRight}');
    const closed = screen.getByRole('tab', { name: 'Closed 18' });
    expect(closed).toHaveFocus();
    expect(closed).toHaveAttribute('aria-selected', 'false');
    await user.keyboard('{Enter}');
    expect(closed).toHaveAttribute('aria-selected', 'true');
    expect(panel).toHaveTextContent('Showing closed');
    await user.click(screen.getByRole('tab', { name: 'All 24' }));
    expect(panel).toHaveTextContent('Showing all');
    await expectNoA11yViolations();
  });

  it('keeps one tab stop when the value matches no option', () => {
    render(<TabsFilter aria-label="Status" options={STATUS} value="archived" />);
    const stops = screen.getAllByRole('tab').filter((t) => t.getAttribute('tabindex') === '0');
    expect(stops.map((t) => t.textContent)).toEqual(['All24']);
  });

  it('renders on the server', () => {
    const html = renderToString(
      <TabsFilter aria-label="Status" options={STATUS} defaultValue="open">
        list
      </TabsFilter>,
    );
    expect(html).toContain('role="tabpanel"');
    expect(html).toContain('mn-tabs__count');
  });
});

describe('useTabsUrlBinding', () => {
  const values = ['overview', 'activity', 'files'] as const;

  it('maps missing and unknown values to the default', () => {
    const onChange = vi.fn();
    const { result, rerender } = renderHook(
      ({ value }: { value: string | null }) =>
        useTabsUrlBinding({ value, onChange, values, defaultValue: 'overview' }),
      { initialProps: { value: null as string | null } },
    );
    expect(result.current.value).toBe('overview');
    rerender({ value: 'nope' });
    expect(result.current.value).toBe('overview');
    rerender({ value: 'files' });
    expect(result.current.value).toBe('files');
  });

  it('writes values back, clearing the default unless told not to', () => {
    const onChange = vi.fn();
    const { result, rerender } = renderHook(
      ({ clearDefault }: { clearDefault: boolean }) =>
        useTabsUrlBinding({
          value: 'files',
          onChange,
          values,
          defaultValue: 'overview',
          clearDefault,
        }),
      { initialProps: { clearDefault: true } },
    );
    act(() => result.current.onValueChange('activity'));
    act(() => result.current.onValueChange('overview'));
    act(() => result.current.onValueChange('bogus'));
    rerender({ clearDefault: false });
    act(() => result.current.onValueChange('overview'));
    expect(onChange.mock.calls.map((c) => c[0])).toEqual(['activity', null, 'overview']);
  });

  it('drives Tabs from an injected URL value', async () => {
    function Page() {
      const [param, setParam] = useState<string | null>('activity');
      const tabs = useTabsUrlBinding({
        value: param,
        onChange: setParam,
        values,
        defaultValue: 'overview',
      });
      return (
        <>
          <Tabs {...tabs}>
            <TabsList aria-label="Sections">
              {values.map((v) => (
                <TabsTrigger key={v} value={v}>
                  {v}
                </TabsTrigger>
              ))}
            </TabsList>
            {values.map((v) => (
              <TabsContent key={v} value={v}>
                {v} panel
              </TabsContent>
            ))}
          </Tabs>
          <output>{param ?? '(none)'}</output>
        </>
      );
    }
    render(<Page />);
    expect(screen.getByRole('tabpanel')).toHaveTextContent('activity panel');
    await userEvent.setup().click(screen.getByRole('tab', { name: 'overview' }));
    expect(screen.getByRole('status')).toHaveTextContent('(none)');
    expect(screen.getByRole('tabpanel')).toHaveTextContent('overview panel');
  });
});
