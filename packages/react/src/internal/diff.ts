/**
 * Longest-common-subsequence diff core shared by the line and word diffs
 * (VersionHistoryPanel's `diffVersionLines`, AiArtifactVersions' `diffLines`,
 * AiEditProposal's `diffWords`). Each caller tokenises its input and shapes
 * the result; this file only decides which items are kept, removed or added.
 */

/** One step from `a` to `b`: keep an item of both, delete one of `a`, insert one of `b`. */
export interface DiffOp<T> {
  type: 'equal' | 'delete' | 'insert';
  value: T;
}

/**
 * The edit script from `a` to `b` along a longest common subsequence. When
 * both an a-deletion and a b-insertion are optimal, the deletion comes first.
 * Inputs with more than `maxPairs` item pairs (`a.length * b.length`) skip the
 * O(n·m) table and fall back to deleting all of `a`, then inserting all of `b`.
 */
export function diffSequences<T>(
  a: readonly T[],
  b: readonly T[],
  maxPairs = Number.POSITIVE_INFINITY,
): DiffOp<T>[] {
  const n = a.length;
  const m = b.length;
  const out: DiffOp<T>[] = [];
  if (n * m > maxPairs) {
    for (const value of a) out.push({ type: 'delete', value });
    for (const value of b) out.push({ type: 'insert', value });
    return out;
  }
  // lcs[i * w + j] = LCS length of a[i..] and b[j..].
  const w = m + 1;
  const lcs = new Uint32Array((n + 1) * w);
  for (let i = n - 1; i >= 0; i--) {
    for (let j = m - 1; j >= 0; j--) {
      lcs[i * w + j] =
        a[i] === b[j]
          ? (lcs[(i + 1) * w + j + 1] ?? 0) + 1
          : Math.max(lcs[(i + 1) * w + j] ?? 0, lcs[i * w + j + 1] ?? 0);
    }
  }
  let i = 0;
  let j = 0;
  while (i < n && j < m) {
    if (a[i] === b[j]) {
      out.push({ type: 'equal', value: a[i] as T });
      i++;
      j++;
    } else if ((lcs[(i + 1) * w + j] ?? 0) >= (lcs[i * w + j + 1] ?? 0)) {
      out.push({ type: 'delete', value: a[i++] as T });
    } else {
      out.push({ type: 'insert', value: b[j++] as T });
    }
  }
  while (i < n) out.push({ type: 'delete', value: a[i++] as T });
  while (j < m) out.push({ type: 'insert', value: b[j++] as T });
  return out;
}
