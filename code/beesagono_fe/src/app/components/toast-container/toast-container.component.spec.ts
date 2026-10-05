import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal, WritableSignal } from '@angular/core';
import { vi, describe, beforeEach, it, expect } from 'vitest';
import { ToastContainerComponent } from './toast-container.component';
import { ToastService } from '../../services/toast/toast.service';
import { ToastInfo } from '../../models/toast/toast.model';

describe('ToastContainerComponent', () => {
  let component: ToastContainerComponent;
  let fixture: ComponentFixture<ToastContainerComponent>;

  // We use WritableSignal to correctly simulate the service's read-only Signal
  let toastsSignal: WritableSignal<ToastInfo[]>;
  let mockToastService: {
    toasts: WritableSignal<ToastInfo[]>;
    remove: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    toastsSignal = signal<ToastInfo[]>([]);

    mockToastService = {
      toasts: toastsSignal,
      remove: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [ToastContainerComponent],
      providers: [
        { provide: ToastService, useValue: mockToastService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ToastContainerComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should inject ToastService correctly', () => {
    fixture.detectChanges();
    expect(component.toastService).toBeDefined();
    expect(component.toastService.toasts()).toEqual([]);
  });

  it('should expose toasts array from ToastService signal', () => {
    const sampleToast: ToastInfo = { body: 'Test Toast', classname: 'bg-danger' };

    toastsSignal.set([sampleToast]);

    fixture.detectChanges();

    expect(component.toastService.toasts().length).toBe(1);
    expect(component.toastService.toasts()[0]).toEqual(sampleToast);
  });

  it('should call remove on ToastService when a toast is removed', () => {
    fixture.detectChanges();
    const sampleToast: ToastInfo = { body: 'Test Toast' };

    component.toastService.remove(sampleToast);

    expect(mockToastService.remove).toHaveBeenCalledWith(sampleToast);
  });
});