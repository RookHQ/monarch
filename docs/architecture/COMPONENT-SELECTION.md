# Who Monarch serves and how components are chosen

Monarch is the design system behind the web applications of Rook Platforms. It
is also published as open source (MIT), so anyone can use the tokens, the
React components and the conformance tool.

## Who it serves

- **Rook product teams** building authenticated applications (lists, forms,
  dashboards, settings, inboxes) and public pages (marketing, status, booking
  and careers pages) that must feel like one product family.
- **Other teams** that want an accessible, token-driven React component set
  with a contrast-checked colour system and a tool that measures how closely an
  application follows it.

Every Rook product shares the same needs: accessible forms and dialogs, dense
data tables, status and feedback, navigation shells, and per-product brand
colours that never break contrast. Monarch exists so those are built once.

## How a component earns a place

A component is **in scope** for the registry when either:

1. **three or more** products need it, or
2. **two or more** need it and at least one needs it urgently (priority P0).

A third route is **strategic scope**
([ADR 0011](../adr/0011-strategic-scope.md)): a family that Rook leadership
designates ahead of demand, because waiting for three products to build their
own copies is the problem it solves. Strategic families are listed in
[`registry/strategic.json`](../../registry/strategic.json) and appear in the
registry with `scope: "strategic"`. The first one is Destructive &
confirmation (wave 10). Strategic components meet the same contract as every
other component.

Components below that rule are either parts of an in-scope family (a chart
frame under every chart) or are left to the product that needs them until a
second or third product asks for the same thing. The rule keeps product-specific
workflows out of the shared library: Monarch components take data and
callbacks, and business logic stays in the product.

The demand data behind the rule is
[`registry/component-demand.json`](../../registry/component-demand.json): per
component, how many products need it, a priority, the nearest existing Monarch
component, and the capabilities Monarch still lacks. It deliberately holds
counts and generic capability names only. The generated
[`registry/components.json`](../../registry/components.json) joins it with what
the packages export today.

<!-- generated:selection-totals -->

| Measure                             | Value |
| ----------------------------------- | ----- |
| Registry entries                    | 165   |
| Needed by 3+ products               | 78    |
| Needed by exactly 2 products at P0  | 10    |
| In scope (3+ products, or 2+ at P0) | 88    |

<!-- /generated:selection-totals -->

How many products need each component in the registry:

<!-- generated:selection-distribution -->

| Products needing it | Components in the registry |
| ------------------- | -------------------------- |
| 1                   | 16                         |
| 2                   | 18                         |
| 3                   | 25                         |
| 4                   | 11                         |
| 5                   | 11                         |
| 6+                  | 31                         |

<!-- /generated:selection-distribution -->

## Where to go next

- [COMPONENT-TAXONOMY](COMPONENT-TAXONOMY.md): categories, layers, naming.
- [COMPONENT-GAP-ANALYSIS](COMPONENT-GAP-ANALYSIS.md): in-scope components
  against today's exports.
- [COMPONENT-IMPLEMENTATION-ROADMAP](../roadmap/COMPONENT-IMPLEMENTATION-ROADMAP.md):
  build order.
- [CONTRIBUTION-GUIDELINES](../governance/CONTRIBUTION-GUIDELINES.md):
  proposing a new component.
