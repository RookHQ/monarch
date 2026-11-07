/** One run of a word diff: unchanged, added or removed text. */
export interface AiDiffSegment {
  type: 'equal' | 'insert' | 'delete';
  text: string;
  /** Identifies a change, so a delete and an insert can be accepted together. */
  id?: string | undefined;
}

/** Words (letters, marks and digits, with inner apostrophes), whitespace runs, and single punctuation marks. */
const TOKEN = /\s+|[\p{L}\p{M}\p{N}_]+(?:['’][\p{L}\p{M}\p{N}_]+)*|[^\s]/gu;

/** Above this many LCS cells the middle of the diff falls back to one delete and one insert. */
const MAX_CELLS = 2_000_000;

function tokenize(text: string): string[] {
  return text.match(TOKEN) ?? [];
}

function push(out: AiDiffSegment[], type: AiDiffSegment['type'], text: string) {
  if (!text) return;
  const last = out[out.length - 1];
  if (last && last.type === type) last.text += text;
  else out.push({ type, text });
}

/**
 * A word-level diff of `before` and `after` (longest common subsequence over
 * words, whitespace and punctuation). Adjacent changes are grouped so each
 * change reads as one removal followed by one insertion, and whitespace alone
 * never splits a change in two. Returns `[]` when both strings are empty.
 *
 * `diffWords('The quick fox', 'The slow fox')` →
 * `[{ type: 'equal', text: 'The ' }, { type: 'delete', text: 'quick' },
 *   { type: 'insert', text: 'slow' }, { type: 'equal', text: ' fox' }]`
 */
export function diffWords(before: string, after: string): AiDiffSegment[] {
  const a = tokenize(before);
  const b = tokenize(after);
  let start = 0;
  while (start < a.length && start < b.length && a[start] === b[start]) start++;
  let endA = a.length;
  let endB = b.length;
  while (endA > start && endB > start && a[endA - 1] === b[endB - 1]) {
    endA--;
    endB--;
  }

  const raw: AiDiffSegment[] = [];
  push(raw, 'equal', a.slice(0, start).join(''));

  const n = endA - start;
  const m = endB - start;
  if (n > 0 && m > 0 && (n + 1) * (m + 1) <= MAX_CELLS) {
    // lcs[i * (m + 1) + j] = LCS length of a[start + i..endA) and b[start + j..endB).
    const w = m + 1;
    const lcs = new Uint32Array((n + 1) * w);
    for (let i = n - 1; i >= 0; i--) {
      for (let j = m - 1; j >= 0; j--) {
        lcs[i * w + j] =
          a[start + i] === b[start + j]
            ? (lcs[(i + 1) * w + j + 1] ?? 0) + 1
            : Math.max(lcs[(i + 1) * w + j] ?? 0, lcs[i * w + j + 1] ?? 0);
      }
    }
    let i = 0;
    let j = 0;
    while (i < n && j < m) {
      const ta = a[start + i] ?? '';
      const tb = b[start + j] ?? '';
      if (ta === tb) {
        push(raw, 'equal', ta);
        i++;
        j++;
      } else if ((lcs[(i + 1) * w + j] ?? 0) >= (lcs[i * w + j + 1] ?? 0)) {
        push(raw, 'delete', ta);
        i++;
      } else {
        push(raw, 'insert', tb);
        j++;
      }
    }
    push(raw, 'delete', a.slice(start + i, endA).join(''));
    push(raw, 'insert', b.slice(start + j, endB).join(''));
  } else {
    push(raw, 'delete', a.slice(start, endA).join(''));
    push(raw, 'insert', b.slice(start, endB).join(''));
  }
  push(raw, 'equal', a.slice(endA).join(''));

  // Whitespace alone between two changes joins them into one.
  const joined = raw.map((seg, k) => {
    const prev = raw[k - 1];
    const next = raw[k + 1];
    const between = prev && next && prev.type !== 'equal' && next.type !== 'equal';
    return seg.type === 'equal' && between && /^\s*$/.test(seg.text)
      ? { ...seg, type: 'replace' as const }
      : seg;
  });

  // Each run of changes becomes one delete followed by one insert.
  const out: AiDiffSegment[] = [];
  let del = '';
  let ins = '';
  const flush = () => {
    push(out, 'delete', del);
    push(out, 'insert', ins);
    del = '';
    ins = '';
  };
  for (const seg of joined) {
    if (seg.type === 'equal') {
      flush();
      push(out, 'equal', seg.text);
    } else if (seg.type === 'delete') del += seg.text;
    else if (seg.type === 'insert') ins += seg.text;
    else {
      del += seg.text;
      ins += seg.text;
    }
  }
  flush();
  return out;
}
