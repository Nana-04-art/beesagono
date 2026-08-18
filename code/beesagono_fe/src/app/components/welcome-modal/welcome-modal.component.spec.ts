import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WelcomeModalComponent } from './welcome-modal.component';
import { WelcomeNoticeService } from '../../services/welcome-notice/welcome-notice.service';
import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { signal } from '@angular/core';

class MockWelcomeNoticeService {
  isNoticeOpen = signal<boolean>(false);
  dismissNoticeCalled = false;

  dismissNotice(): void {
    this.dismissNoticeCalled = true;
    this.isNoticeOpen.set(false);
  }
}

describe('WelcomeModalComponent', () => {
  let component: WelcomeModalComponent;
  let fixture: ComponentFixture<WelcomeModalComponent>;
  let mockWelcomeNoticeService: MockWelcomeNoticeService;

  beforeEach(async () => {
    vi.useFakeTimers();
    mockWelcomeNoticeService = new MockWelcomeNoticeService();

    await TestBed.configureTestingModule({
      imports: [WelcomeModalComponent],
      providers: [
        { provide: WelcomeNoticeService, useValue: mockWelcomeNoticeService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WelcomeModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    document.body.style.overflow = '';
    vi.useRealTimers();
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  describe('Effect and body scroll management', () => {
    it('should set overflow: hidden on body when modal is open', async () => {
      mockWelcomeNoticeService.isNoticeOpen.set(true);
      fixture.detectChanges();
      vi.advanceTimersByTime(50);
      await fixture.whenStable();

      expect(document.body.style.overflow).toBe('hidden');
    });

    it('should restore body overflow when modal is closed', async () => {
      mockWelcomeNoticeService.isNoticeOpen.set(true);
      fixture.detectChanges();
      vi.advanceTimersByTime(50);
      await fixture.whenStable();

      mockWelcomeNoticeService.isNoticeOpen.set(false);
      fixture.detectChanges();
      vi.advanceTimersByTime(50);
      await fixture.whenStable();

      expect(document.body.style.overflow).toBe('');
    });
  });

  describe('dismissNotice()', () => {
    it('should restore body scroll and call service dismissNotice', () => {
      document.body.style.overflow = 'hidden';

      component.dismissNotice();

      expect(document.body.style.overflow).toBe('');
      expect(mockWelcomeNoticeService.dismissNoticeCalled).toBe(true);
    });
  });

  describe('Keyboard Event Handling (@HostListener)', () => {
    beforeEach(() => {
      mockWelcomeNoticeService.isNoticeOpen.set(true);
      fixture.detectChanges();
    });

    it('should trap Tab key and prevent default behavior', async () => {
      let isDefaultPrevented = false;
      const event = {
        key: 'Tab',
        preventDefault: () => { isDefaultPrevented = true; }
      } as KeyboardEvent;

      component.handleKeyboardEvent(event);
      vi.advanceTimersByTime(50);
      await fixture.whenStable();

      expect(isDefaultPrevented).toBe(true);
    });

    it('should catch Escape key, prevent default and close modal', () => {
      let isDefaultPrevented = false;
      const event = {
        key: 'Escape',
        preventDefault: () => { isDefaultPrevented = true; }
      } as KeyboardEvent;

      component.handleKeyboardEvent(event);

      expect(isDefaultPrevented).toBe(true);
      expect(mockWelcomeNoticeService.dismissNoticeCalled).toBe(true);
    });

    it('should do nothing on key presses when modal is closed', () => {
      mockWelcomeNoticeService.isNoticeOpen.set(false);
      fixture.detectChanges();

      let isDefaultPrevented = false;
      const event = {
        key: 'Tab',
        preventDefault: () => { isDefaultPrevented = true; }
      } as KeyboardEvent;

      component.handleKeyboardEvent(event);

      expect(isDefaultPrevented).toBe(false);
    });
  });

  describe('Focus Management', () => {
    it('should focus confirmBtn after modal opens', async () => {
      mockWelcomeNoticeService.isNoticeOpen.set(true);
      fixture.detectChanges();

      if (component.confirmBtn?.nativeElement) {
        const focusSpy = vi.spyOn(component.confirmBtn.nativeElement, 'focus');

        vi.advanceTimersByTime(100);
        await fixture.whenStable();

        expect(focusSpy).toHaveBeenCalled();
      } else {
        expect(component.confirmBtn).toBeDefined();
      }
    });
  });
});