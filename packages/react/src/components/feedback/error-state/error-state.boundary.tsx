'use client';

import { Component, type ErrorInfo, type ReactNode } from 'react';
import { ErrorState, type ErrorStateProps } from './error-state.js';

// Default fallback: an mn-error-state (ErrorState) with a retry that resets the boundary.

export interface ErrorStateBoundaryFallbackProps {
  /** The error that was thrown. Log it; never show its message to the person. */
  error: Error;
  /** Clears the error and renders the children again. */
  reset: () => void;
}

export interface ErrorStateBoundaryProps extends Pick<
  ErrorStateProps,
  'title' | 'description' | 'retryLabel' | 'headingLevel' | 'size' | 'requestId' | 'requestIdLabel'
> {
  children?: ReactNode;
  /**
   * Replaces the default fallback (an ErrorState with a retry that resets the
   * boundary). A function receives the error and `reset`.
   */
  fallback?: ReactNode | ((props: ErrorStateBoundaryFallbackProps) => ReactNode);
  /** Report the error (logging, monitoring). Called once per caught error. */
  onError?: (error: Error, info: ErrorInfo) => void;
  /** Called after the boundary resets, from the retry button or a `resetKeys` change. */
  onReset?: () => void;
  /**
   * When any value changes (by `Object.is`) while an error is shown, the
   * boundary resets. Pass the inputs the failed panel depends on, such as the
   * selected record id.
   */
  resetKeys?: readonly unknown[];
}

interface BoundaryState {
  error: Error | null;
}

const changed = (a: readonly unknown[] = [], b: readonly unknown[] = []) =>
  a.length !== b.length || a.some((v, i) => !Object.is(v, b[i]));

/**
 * Error boundary for one panel or region: a render error inside it shows an
 * ErrorState with "Try again" instead of taking down the page. The rest of the
 * page keeps working.
 */
export class ErrorStateBoundary extends Component<ErrorStateBoundaryProps, BoundaryState> {
  static displayName = 'ErrorStateBoundary';

  override state: BoundaryState = { error: null };

  static getDerivedStateFromError(raw: unknown): BoundaryState {
    return { error: raw instanceof Error ? raw : new Error(String(raw)) };
  }

  override componentDidCatch(error: Error, info: ErrorInfo) {
    this.props.onError?.(error, info);
  }

  override componentDidUpdate(prev: ErrorStateBoundaryProps) {
    if (this.state.error && changed(prev.resetKeys, this.props.resetKeys)) this.reset();
  }

  reset = () => {
    this.setState({ error: null });
    this.props.onReset?.();
  };

  override render() {
    const { error } = this.state;
    if (!error) return this.props.children;
    const {
      fallback,
      children: _children,
      onError: _e,
      onReset: _r,
      resetKeys: _k,
      ...rest
    } = this.props;
    if (typeof fallback === 'function') return fallback({ error, reset: this.reset });
    if (fallback !== undefined) return fallback;
    return <ErrorState {...rest} onRetry={this.reset} />;
  }
}
