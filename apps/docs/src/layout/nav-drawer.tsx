import { Sheet, SheetBody, SheetContent, SheetHeader, SheetTitle } from '@rookcom/monarch-react';
import { Sidebar } from './sidebar.js';

/** The navigation drawer of narrow screens; docs-shell.tsx loads it on first use. */
export function NavDrawer({
  path,
  open,
  onOpenChange,
}: {
  path: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="left" size="sm" className="docs-drawer">
        <SheetHeader>
          <SheetTitle>Monarch</SheetTitle>
        </SheetHeader>
        <SheetBody data-scroll-box>
          <Sidebar path={path} collapsed />
        </SheetBody>
      </SheetContent>
    </Sheet>
  );
}
