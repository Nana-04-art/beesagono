import { Injectable } from '@angular/core';
import { ToastInfo } from '../../models/toast/toast.model';


@Injectable({
  providedIn: 'root'
})
export class ToastService {
  toasts: ToastInfo[] = [];

  show(body: string, options: any = {}) {
    const toast = { body, ...options };
    this.toasts.push(toast);

    // Auto-removal after the delay (default 3 seconds)
    setTimeout(() => {
      this.remove(toast);
    }, options.delay || 3000);
  }

  remove(toast: ToastInfo) {
    this.toasts = this.toasts.filter(t => t !== toast);
  }
}
