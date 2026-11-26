/**
 * What the thread's announcer says for a reply: its Markdown turned into
 * speakable text the way AiStreamingText does it (code blocks and tables are
 * named, not read out character by character). Parsed incrementally, with
 * finished blocks cached, so it stays cheap while a reply streams.
 */
import {
  blockText,
  parseMarkdownIncremental,
  type MarkdownBlock,
  type MarkdownParseState,
} from '../ai-markdown/index.js';
import { messageText, type AiMessage } from '../ai-conversation/index.js';

export interface SpeechLabels {
  codeBlock: string;
  table: string;
}

export interface SpeechState {
  id: string;
  parse: MarkdownParseState | null;
  labels: SpeechLabels;
}

const spokenCache = new WeakMap<MarkdownBlock, string>();

/** The spoken text of a message, and the state to pass back next time. */
export function spokenMessage(
  message: AiMessage,
  previous: SpeechState | null,
  labels: SpeechLabels,
): { text: string; state: SpeechState } {
  const reuse =
    previous &&
    previous.id === message.id &&
    previous.labels.codeBlock === labels.codeBlock &&
    previous.labels.table === labels.table;
  const parse = parseMarkdownIncremental(messageText(message), reuse ? previous.parse : null);
  const options = { code: () => labels.codeBlock, table: () => labels.table };
  const blocks = parse.blocks;
  const out: string[] = [];
  blocks.forEach((b, i) => {
    // The last block may still grow: never cache it.
    const last = i === blocks.length - 1;
    let value = last || !reuse ? undefined : spokenCache.get(b);
    if (value === undefined) {
      value = blockText(b, options).trim();
      if (!last) spokenCache.set(b, value);
    }
    if (value) out.push(value);
  });
  return { text: out.join('\n\n'), state: { id: message.id, parse, labels } };
}
