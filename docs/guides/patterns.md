# Experience patterns

Patterns are compositions of Monarch components for screens most
applications have. They carry layout, semantics and accessibility; data fetching,
routing and state stay in the app. All are exported from
`@rookcom/monarch-react`.

| Pattern                                      | Use it for                                                                                                           | Typical screen            |
| -------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- | ------------------------- |
| `PageHeader`                                 | The top of every main view: one `h1`, context, page-level actions, optional tabs                                     | Project or pipeline views |
| `Toolbar` + `ToolbarGroup`                   | Controls that act on a list or board: search, view switch, density, export. `role="toolbar"`, arrow-key roving focus | Inbox, board              |
| `FilterBar` + `FilterChip`                   | Active filters as removable chips with "Clear all"                                                                   | Any filterable list       |
| `Table` + `Pagination`                       | Sortable, selectable data tables                                                                                     | Lists and admin tables    |
| `SplitLayout`                                | List + detail panes (stacked under `md`)                                                                             | Inbox, chat, documents    |
| `SettingsSection` + `SettingsRow`            | Titled settings groups with one control per row; `tone="danger"` for destructive zones                               | Settings pages            |
| `DescriptionList` + `DescriptionItem`        | Key/value facts on detail pages                                                                                      | Record detail pages       |
| `StatCard`                                   | A single KPI with delta and period                                                                                   | Dashboards                |
| `EmptyState` / `ErrorState` / `LoadingState` | The three non-happy states of any list or detail view                                                                | Every list or detail view |

## Inbox

```tsx
<PageHeader title="Inbox" actions={<Button variant="primary">New ticket</Button>} />
<Toolbar
  label="Inbox tools"
  search={<Input aria-label="Search tickets" placeholder="Search…" />}
  end={<Button size="sm">Export</Button>}
  filters={
    <FilterBar onClearAll={clear}>
      <FilterChip name="Status" value="Open" onRemove={() => remove('status')} />
    </FilterBar>
  }
>
  <Button size="sm">Sort</Button>
</Toolbar>
<SplitLayout
  listLabel="Tickets"
  detailLabel="Ticket"
  list={<TicketList />}
  detail={selected ? <TicketDetail id={selected} /> : <EmptyState title="Select a ticket" />}
  detailOpen={Boolean(selected)}
  onBack={() => setSelected(null)}
/>
```

## Data table

```tsx
<Card flush>
  <Table aria-label="Tasks" density="compact" stickyHeader>
    <TableHeader>
      <TableRow>
        <TableSelectCell header label="Select all" checked={allChecked} onCheckedChange={toggleAll} />
        <TableHead sort={sort.title} onSortChange={(d) => setSort({ title: d })}>Title</TableHead>
        <TableHead numeric>Points</TableHead>
      </TableRow>
    </TableHeader>
    <TableBody>
      {rows.map((r) => (
        <TableRow key={r.id} selected={isSelected(r.id)}>
          <TableSelectCell label={`Select ${r.title}`} checked={isSelected(r.id)} onCheckedChange={() => toggle(r.id)} />
          <TableCell>{r.title}</TableCell>
          <TableCell numeric>{r.points}</TableCell>
        </TableRow>
      ))}
    </TableBody>
  </Table>
</Card>
<Pagination variant="compact" page={page} pageCount={pages} pageSize={50} totalItems={total} onPageChange={setPage} />
```

## Settings (Workspace)

```tsx
<SettingsSection title="Profile" description="What teammates see." actions={<Button variant="primary">Save</Button>}>
  <SettingsRow label="Display name" required error={errors.name}>
    <Input value={name} onChange={…} />
  </SettingsRow>
  <SettingsRow label="Weekly digest" description="Sent on Mondays.">
    <Switch checked={digest} onCheckedChange={setDigest} />
  </SettingsRow>
</SettingsSection>
<SettingsSection title="Delete workspace" tone="danger" description="This cannot be undone.">
  <SettingsRow label="Delete">
    <Button variant="danger">Delete workspace…</Button>
  </SettingsRow>
</SettingsSection>
```

## Dashboard

```tsx
<div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 'var(--mn-space-4)' }}>
  <StatCard label="Pipeline" value="₹12.4L" delta="+12%" deltaDirection="up" caption="vs last 30 days" />
  <StatCard label="Churn" value="2.1%" delta="+0.3%" deltaDirection="up" deltaTone="danger" />
</div>
<DescriptionList columns={3}>
  <DescriptionItem term="Owner">Ada Lovelace</DescriptionItem>
  <DescriptionItem term="Stage">Proposal</DescriptionItem>
</DescriptionList>
```

## Toasts

```tsx
// root
<ToastProvider>{children}</ToastProvider>;
// anywhere
const { toast } = useToast();
toast({ title: 'Ticket archived', tone: 'success', action: { label: 'Undo', onClick: undo } });
```

Rules: `success`/`info` toasts are polite status messages; `danger` is an
`alert`. At most three are visible. Never put the only path to recover in a
toast; errors that block work belong in an `Alert` or `ErrorState` on the page.
