import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { signal } from '@angular/core';
import { WelcomeModalComponent } from './welcome-modal.component';
import { WelcomeNoticeService } from '../../services/welcome-notice/welcome-notice.service';

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
  });

  it('should create the component', () => {
    expect(component).toBeTruthy();
  });

  describe('Effect and body scroll management', () => {
    it('should set overflow: hidden on body when modal is open', fakeAsync(() => {
      mockWelcomeNoticeService.isNoticeOpen.set(true);
      fixture.detectChanges();
      tick(50);

      expect(document.body.style.overflow).toBe('hidden');
    }));

    it('should restore body overflow when modal is closed', fakeAsync(() => {
      mockWelcomeNoticeService.isNoticeOpen.set(true);
      fixture.detectChanges();
      tick(50);

      mockWelcomeNoticeService.isNoticeOpen.set(false);
      fixture.detectChanges();

      expect(document.body.style.overflow).toBe('');
    }));
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

    it('should trap Tab key and prevent default behavior', fakeAsync(() => {
      let isDefaultPrevented = false;
      const event = {
        key: 'Tab',
        preventDefault: () => { isDefaultPrevented = true; }
      } as KeyboardEvent;

      component.handleKeyboardEvent(event);
      tick(50);

      expect(isDefaultPrevented).toBe(true);
    }));

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
    it('should focus confirmBtn after modal opens', fakeAsync(() => {
      let focused = false;
      const buttonNativeEl = {
        focus: () => { focused = true; }
      } as unknown as HTMLButtonElement;

      component.confirmBtn = { nativeElement: buttonNativeEl };

      mockWelcomeNoticeService.isNoticeOpen.set(true);
      fixture.detectChanges();
      tick(50);

      expect(focused).toBe(true);
    }));
  });
});