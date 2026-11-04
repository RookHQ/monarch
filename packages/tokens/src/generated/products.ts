// Generated from registry/products by scripts/generate.ts. Do not edit.
import { hsl, type Hsl } from '../color.js';

export interface ProductAccent {
  readonly id: string;
  readonly name: string;
  readonly accent: Hsl;
}

export const products = {
  "app": { id: "app", name: "Rook App", accent: hsl(354, 70, 52) },
  "atlas": { id: "atlas", name: "Rook Atlas", accent: hsl(25, 95, 53) },
  "base": { id: "base", name: "Rook Base", accent: hsl(44, 100, 48) },
  "calendar": { id: "calendar", name: "Rook Calendar", accent: hsl(198, 100, 42) },
  "chat": { id: "chat", name: "Rook Chat", accent: hsl(239, 84, 67) },
  "dossier": { id: "dossier", name: "Rook Dossier", accent: hsl(175, 84, 32) },
  "dots": { id: "dots", name: "Rook Dots", accent: hsl(354, 70, 52) },
  "friends": { id: "friends", name: "Rook Friends", accent: hsl(0, 0, 0) },
  "gtm": { id: "gtm", name: "Rook GTM", accent: hsl(25, 95, 53) },
  "id": { id: "id", name: "Rook ID & Workspace", accent: hsl(354, 70, 52) },
  "intellect": { id: "intellect", name: "Rook Intellect", accent: hsl(175, 77, 26) },
  "links": { id: "links", name: "Rook Links", accent: hsl(136, 53, 43) },
  "people": { id: "people", name: "Rook People", accent: hsl(227, 74, 53) },
  "pulse": { id: "pulse", name: "Rook Pulse", accent: hsl(326, 72, 44) },
  "ranker": { id: "ranker", name: "Rook Ranker", accent: hsl(239, 84, 67) },
  "recruit": { id: "recruit", name: "Rook Recruit", accent: hsl(44, 100, 48) },
  "signal": { id: "signal", name: "Rook Signal", accent: hsl(198, 100, 42) },
  "stride": { id: "stride", name: "Rook Stride", accent: hsl(136, 63, 42) },
  "tickets": { id: "tickets", name: "Rook Tickets", accent: hsl(354, 71, 52) },
} as const satisfies Record<string, ProductAccent>;

export type ProductId = keyof typeof products;
export const productIds = Object.keys(products) as ProductId[];
