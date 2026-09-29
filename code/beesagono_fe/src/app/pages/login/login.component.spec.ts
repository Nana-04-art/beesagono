import { ElementRef, WritableSignal, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { LoginComponent } from './login.component';
import { AuthService } from '../../services/auth/auth.service';
import { GoogleAuthService } from '../../services/auth/google-auth.service';
import { ToastService } from '../../services/toast/toast.service';
import { GoogleCheckResponse, LoginResponse } from '../../models/auth/auth-dto.model';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let router: Router;

  let pendingGoogleUserSignal: WritableSignal<GoogleCheckResponse | null>;
  let selectedUsernameSignal: WritableSignal<string>;
  let isLoadingSignal: WritableSignal<boolean>;

  let mockAuthService: {
    login: ReturnType<typeof vi.fn>;
  };

  let mockGoogleAuthService: {
    pendingGoogleUser: WritableSignal<GoogleCheckResponse | null>;
    selectedUsername: WritableSignal<string>;
    isLoading: WritableSignal<boolean>;
    initializeGoogleButton: ReturnType<typeof vi.fn>;
    cancelGoogleRegistration: ReturnType<typeof vi.fn>;
    completeGoogleRegistration: ReturnType<typeof vi.fn>;
  };

  let mockToastService: {
    clear: ReturnType<typeof vi.fn>;
    show: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    (globalThis as any).google = {
      accounts: {
        id: {
          initialize: vi.fn(),
          renderButton: vi.fn(),
        },
      },
    };

    pendingGoogleUserSignal = signal<GoogleCheckResponse | null>(null);
    selectedUsernameSignal = signal<string>('');
    isLoadingSignal = signal<boolean>(false);

    mockAuthService = {
      login: vi.fn(),
    };

    mockGoogleAuthService = {
      pendingGoogleUser: pendingGoogleUserSignal,
      selectedUsername: selectedUsernameSignal,
      isLoading: isLoadingSignal,
      initializeGoogleButton: vi.fn(),
      cancelGoogleRegistration: vi.fn(),
      completeGoogleRegistration: vi.fn(),
    };

    mockToastService = {
      clear: vi.fn(),
      show: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: mockAuthService },
        { provide: GoogleAuthService, useValue: mockGoogleAuthService },
        { provide: ToastService, useValue: mockToastService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);

    vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    delete (globalThis as any).google;
    document.body.style.overflow = '';
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngAfterViewInit', () => {
    it('should initialize Google button with correct client ID', () => {
      expect(mockGoogleAuthService.initializeGoogleButton).toHaveBeenCalledWith(
        'googleBtn',
        '362890069300-870q1d3b9nj16i0j19gco6npvcma1hpr.apps.googleusercontent.com'
      );
    });
  });

  describe('Constructor effect (Modal pending google user)', () => {
    it('should lock body scroll and focus input when pendingGoogleUser is present', async () => {
      const focusInputSpy = vi.spyOn(component as any, 'focusUsernameInput');

      pendingGoogleUserSignal.set({
        registered: false,
        email: 'test@example.com',
      });

      TestBed.flushEffects();
      fixture.detectChanges();

      expect(document.body.style.overflow).toBe('hidden');
      expect(focusInputSpy).toHaveBeenCalled();
    });

    it('should restore body scroll when pendingGoogleUser is null', () => {
      mockGoogleAuthService.pendingGoogleUser.set(null);
      TestBed.flushEffects();
      fixture.detectChanges();

      expect(document.body.style.overflow).toBe('');
    });
  });

  describe('handleKeyboardEvent', () => {
    it('should do nothing if pendingGoogleUser is null', () => {
      const cancelSpy = vi.spyOn(component, 'cancelGoogleModal');
      const event = new KeyboardEvent('keydown', { key: 'Escape' });

      component.handleKeyboardEvent(event);

      expect(cancelSpy).not.toHaveBeenCalled();
    });

    it('should cancel modal on Escape key press', () => {
      pendingGoogleUserSignal.set({ registered: false });
      const cancelSpy = vi.spyOn(component, 'cancelGoogleModal');
      const event = new KeyboardEvent('keydown', { key: 'Escape' });

      component.handleKeyboardEvent(event);

      expect(cancelSpy).toHaveBeenCalled();
    });

    it('should navigate focus trap forward with Tab key', () => {
      pendingGoogleUserSignal.set({ registered: false });

      const inputEl = document.createElement('input');
      const cancelEl = document.createElement('button');
      const completeEl = document.createElement('button');

      component.usernameInputRef = new ElementRef(inputEl);
      component.cancelBtnRef = new ElementRef(cancelEl);
      component.completeBtnRef = new ElementRef(completeEl);

      vi.spyOn(document, 'activeElement', 'get').mockReturnValue(cancelEl);

      const event = new KeyboardEvent('keydown', { key: 'Tab', shiftKey: false });
      const preventDefaultSpy = vi.spyOn(event, 'preventDefault');

      component.handleKeyboardEvent(event);

      expect(preventDefaultSpy).toHaveBeenCalled();

      vi.spyOn(document, 'activeElement', 'get').mockReturnValue(completeEl);
      const inputFocusSpy = vi.spyOn(inputEl, 'focus');

      component.handleKeyboardEvent(event);

      expect(preventDefaultSpy).toHaveBeenCalled();
      expect(inputFocusSpy).toHaveBeenCalled();
    });

    it('should navigate focus trap backward with Shift+Tab key', () => {
      pendingGoogleUserSignal.set({ registered: false });

      const inputEl = document.createElement('input');
      const cancelEl = document.createElement('button');
      const completeEl = document.createElement('button');

      component.usernameInputRef = new ElementRef(inputEl);
      component.cancelBtnRef = new ElementRef(cancelEl);
      component.completeBtnRef = new ElementRef(completeEl);

      vi.spyOn(document, 'activeElement', 'get').mockReturnValue(inputEl);
      const completeFocusSpy = vi.spyOn(completeEl, 'focus');

      const event = new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true });
      const preventDefaultSpy = vi.spyOn(event, 'preventDefault');

      component.handleKeyboardEvent(event);

      expect(preventDefaultSpy).toHaveBeenCalled();
      expect(completeFocusSpy).toHaveBeenCalled();
    });
  });

  describe('cancelGoogleModal', () => {
    it('should reset body overflow and cancel Google registration', () => {
      document.body.style.overflow = 'hidden';

      component.cancelGoogleModal();

      expect(document.body.style.overflow).toBe('');
      expect(mockGoogleAuthService.cancelGoogleRegistration).toHaveBeenCalled();
    });
  });

  describe('onSubmit', () => {
    it('should perform successful login, show toast, and navigate to home', () => {
      component.usernameOrEmail.set('mario');
      component.password.set('password123');

      const mockLoginResponse: LoginResponse = {
        accessToken: 'mock-access-token',
        refreshToken: 'mock-refresh-token',
        tokenType: 'Bearer',
        id: '1',
        username: 'Mario',
        email: 'mario@example.com',
        role: 'USER',
      };

      mockAuthService.login.mockReturnValue(of(mockLoginResponse));

      component.onSubmit();

      expect(mockAuthService.login).toHaveBeenCalledWith({
        usernameOrEmail: 'mario',
        password: 'password123',
      });

      expect(component.isLoading()).toBe(false);

      expect(mockToastService.show).toHaveBeenCalledWith(
        'Bentornato, Mario!',
        { classname: 'bg-success text-white' }
      );

      expect(router.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should handle 401 Unauthorized error', () => {
      component.usernameOrEmail.set('mario');
      component.password.set('wrongpassword');

      mockAuthService.login.mockReturnValue(throwError(() => ({ status: 401 })));

      component.onSubmit();

      expect(component.isLoading()).toBe(false);
      expect(mockToastService.show).toHaveBeenCalledWith(
        'Username/email o password errati.',
        { classname: 'bg-danger text-white' }
      );
    });

    it('should handle generic error', () => {
      component.usernameOrEmail.set('mario');
      component.password.set('password123');

      mockAuthService.login.mockReturnValue(throwError(() => ({ status: 500 })));

      component.onSubmit();

      expect(component.isLoading()).toBe(false);
      expect(mockToastService.show).toHaveBeenCalledWith(
        'Credenziali non valide. Riprova.',
        { classname: 'bg-danger text-white' }
      );
    });
  });
});