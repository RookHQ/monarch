import { Component, type ReactNode } from 'react';

/** Keeps one broken example from taking the whole page down, and says so. */
export class PreviewBoundary extends Component<
  { children: ReactNode; name: string },
  { error: Error | null }
> {
  override state: { error: Error | null } = { error: null };
  static getDerivedStateFromError(error: Error) {
    return { error };
  }
  override render() {
    if (this.state.error)
      return (
        <p className="docs-preview-error" role="status">
          The {this.props.name} example failed to render: {this.state.error.message}
        </p>
      );
    return this.props.children;
  }
}
