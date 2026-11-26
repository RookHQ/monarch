/**
 * The branching message tree behind `useAiConversation`: every regenerate or
 * edit adds a sibling, and the visible conversation is one path through it.
 * Pure functions; the path keeps object identity for unchanged messages so
 * memoised message views do not re-render while a reply streams.
 */
import type { AiMessage } from './ai-conversation.types.js';

export interface AiMessageTree {
  nodes: Readonly<Record<string, AiMessage>>;
  parent: Readonly<Record<string, string | null>>;
  /** Children per parent id (`''` is the root). */
  children: Readonly<Record<string, readonly string[]>>;
  /** Index of the shown child per parent id. */
  active: Readonly<Record<string, number>>;
}

export const ROOT = '';

export function treeFrom(messages: readonly AiMessage[]): AiMessageTree {
  const nodes: Record<string, AiMessage> = {};
  const parent: Record<string, string | null> = {};
  const children: Record<string, string[]> = {};
  const active: Record<string, number> = {};
  let prev = ROOT;
  for (const m of messages) {
    nodes[m.id] = m;
    parent[m.id] = prev || null;
    (children[prev] ??= []).push(m.id);
    active[prev] = 0;
    prev = m.id;
  }
  return { nodes, parent, children, active };
}

export function addChild(tree: AiMessageTree, parentId: string, message: AiMessage): AiMessageTree {
  const kids = [...(tree.children[parentId] ?? []), message.id];
  return {
    nodes: { ...tree.nodes, [message.id]: message },
    parent: { ...tree.parent, [message.id]: parentId || null },
    children: { ...tree.children, [parentId]: kids },
    active: { ...tree.active, [parentId]: kids.length - 1 },
  };
}

/** Replaces one node. Returns the same tree when the node is unchanged or missing. */
export function replaceNode(tree: AiMessageTree, message: AiMessage): AiMessageTree {
  if (tree.nodes[message.id] === message || !tree.nodes[message.id]) return tree;
  return { ...tree, nodes: { ...tree.nodes, [message.id]: message } };
}

/** The ids on the visible path, oldest first. */
export function visibleIds(tree: AiMessageTree): string[] {
  const out: string[] = [];
  let at = ROOT;
  for (;;) {
    const kids = tree.children[at];
    if (!kids?.length) break;
    const id = kids[Math.min(tree.active[at] ?? 0, kids.length - 1)];
    if (id === undefined || !tree.nodes[id]) break;
    out.push(id);
    at = id;
  }
  return out;
}

/** Keeps one branch-annotated copy per node, so the same node gives the same object. */
export type AiBranchCache = WeakMap<AiMessage, AiMessage>;

function withBranch(node: AiMessage, index: number, count: number, cache?: AiBranchCache) {
  const hit = cache?.get(node);
  if (hit?.branch && hit.branch.index === index && hit.branch.count === count) return hit;
  const next = { ...node, branch: { index, count } };
  cache?.set(node, next);
  return next;
}

/**
 * The visible conversation: the active child at every level, oldest first.
 * Messages with alternatives carry `branch`. With a cache, an unchanged node
 * gives the same object every time.
 */
export function visiblePath(tree: AiMessageTree, cache?: AiBranchCache): AiMessage[] {
  const out: AiMessage[] = [];
  let at = ROOT;
  for (;;) {
    const kids = tree.children[at];
    if (!kids?.length) break;
    const id = kids[Math.min(tree.active[at] ?? 0, kids.length - 1)];
    const node = id === undefined ? undefined : tree.nodes[id];
    if (id === undefined || !node) break;
    out.push(kids.length > 1 ? withBranch(node, kids.indexOf(id), kids.length, cache) : node);
    at = id;
  }
  return out;
}

/** The last id on the visible path, or the root. */
export function lastVisibleId(tree: AiMessageTree): string {
  const ids = visibleIds(tree);
  return ids[ids.length - 1] ?? ROOT;
}
