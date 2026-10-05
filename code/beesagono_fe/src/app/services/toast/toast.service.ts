import { Injectable, signal } from '@angular/core';
import { ToastInfo } from '../../models/toast/toast.model';

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  // Private mutable signal
  private readonly _toasts = signal<ToastInfo[]>([]);

  // Public read-only signal for components/templates
  readonly toasts = this._toasts.asReadonly();

  show(body: string, options: Partial<ToastInfo> = {}): void {
    const toast: ToastInfo = { body, ...options };

    // Immutable update of the Signal to trigger zoneless Change Detection
    this._toasts.update((current) => [...current, toast]);

    const delay = options.delay ?? 3000;

    // Auto-removal after specified delay
    setTimeout(() => {
      this.remove(toast);
    }, delay);
  }

  remove(toast: ToastInfo): void {
    // Immutable update using filter
    this._toasts.update((current) => current.filter((t) => t !== toast));
  }

  clear(): void {
    this._toasts.set([]);
  }
}