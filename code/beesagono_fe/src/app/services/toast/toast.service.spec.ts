import { TestBed } from '@angular/core/testing';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { ToastService } from './toast.service';
import { ToastInfo } from '../../models/toast/toast.model';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    // Enable Vitest's fake timers
    vi.useFakeTimers();

    TestBed.configureTestingModule({
      providers: [ToastService],
    });

    service = TestBed.inject(ToastService);
  });

  afterEach(() => {
    // Reset real timers after each test
    vi.useRealTimers();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should add a toast with default delay and auto-remove it after 3000ms', () => {
    service.show('Test Message');

    expect(service.toasts().length).toBe(1);
    expect(service.toasts()[0].body).toBe('Test Message');

    // Advance time by 2999ms: the toast must still be present
    vi.advanceTimersByTime(2999);
    expect(service.toasts().length).toBe(1);

    // Reaches 3000ms: the toast is removed
    vi.advanceTimersByTime(1);
    expect(service.toasts().length).toBe(0);
  });

  it('should respect custom delay options for auto-removal', () => {
    const customDelay = 5000;
    service.show('Custom Delay Message', { delay: customDelay, classname: 'bg-success' });

    expect(service.toasts().length).toBe(1);
    expect(service.toasts()[0]).toEqual({
      body: 'Custom Delay Message',
      delay: customDelay,
      classname: 'bg-success',
    });

    vi.advanceTimersByTime(4999);
    expect(service.toasts().length).toBe(1);

    vi.advanceTimersByTime(1);
    expect(service.toasts().length).toBe(0);
  });

  it('should remove a specific toast when remove() is called directly', () => {
    service.show('First Toast');
    service.show('Second Toast');

    const currentToasts = service.toasts();
    expect(currentToasts.length).toBe(2);

    const toastToRemove = currentToasts[0];
    const toastToKeep = currentToasts[1];

    service.remove(toastToRemove);

    expect(service.toasts().length).toBe(1);
    expect(service.toasts()).not.toContain(toastToRemove);
    expect(service.toasts()).toContain(toastToKeep);
  });

  it('should clear all toasts when clear() is called', () => {
    service.show('Toast 1');
    service.show('Toast 2');
    service.show('Toast 3');

    expect(service.toasts().length).toBe(3);

    service.clear();

    expect(service.toasts().length).toBe(0);
  });

  it('should handle multiple toasts independently with different timers', () => {
    service.show('Toast 1', { delay: 2000 });
    service.show('Toast 2', { delay: 4000 });

    expect(service.toasts().length).toBe(2);

    // After 2000ms, the first toast is removed; the second remains.
    vi.advanceTimersByTime(2000);
    expect(service.toasts().length).toBe(1);
    expect(service.toasts()[0].body).toBe('Toast 2');

    // After another 2000ms, the second one is also removed
    vi.advanceTimersByTime(2000);
    expect(service.toasts().length).toBe(0);
  });
});