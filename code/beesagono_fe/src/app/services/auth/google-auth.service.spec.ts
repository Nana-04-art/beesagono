import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { of, throwError } from 'rxjs';
import { GoogleAuthService } from './google-auth.service';
import { AuthService } from '../auth/auth.service';
import { ToastService } from '../toast/toast.service';
import { GoogleCheckResponse, LoginResponse } from '../../models/auth/auth-dto.model';

describe('GoogleAuthService', () => {
  let service: GoogleAuthService;
  let mockAuthService: {
    checkGoogleUser: ReturnType<typeof vi.fn>;
    registerGoogleUser: ReturnType<typeof vi.fn>;
    setSession: ReturnType<typeof vi.fn>;
  };
  let mockToastService: {
    show: ReturnType<typeof vi.fn>;
  };
  let mockRouter: {
    navigate: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    mockAuthService = {
      checkGoogleUser: vi.fn(),
      registerGoogleUser: vi.fn(),
      setSession: vi.fn(),
    };

    mockToastService = {
      show: vi.fn(),
    };

    mockRouter = {
      navigate: vi.fn(),
    };

    // Spies to clear and monitor localStorage
    vi.spyOn(Storage.prototype, 'setItem');
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        GoogleAuthService,
        { provide: AuthService, useValue: mockAuthService },
        { provide: ToastService, useValue: mockToastService },
        { provide: Router, useValue: mockRouter },
      ],
    });

    service = TestBed.inject(GoogleAuthService);
  });

  afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('initializeGoogleButton', () => {
    it('should initialize and render Google button if window.google is defined and trigger callback', () => {
      // Create parent and target elements to test dynamic width behavior
      const parentElement = document.createElement('div');
      const mockElement = document.createElement('div');
      mockElement.id = 'google-btn';
      parentElement.appendChild(mockElement);
      document.body.appendChild(parentElement);

      // Mock parent clientWidth to test calculation: Math.min(Math.max(350 - 32, 200), 400) = 318
      Object.defineProperty(parentElement, 'clientWidth', {
        value: 350,
        configurable: true,
      });

      const handleResponseSpy = vi.spyOn(service, 'handleGoogleCredentialResponse').mockImplementation(() => { });

      let capturedCallback: (response: any) => void = () => { };
      const mockInitialize = vi.fn().mockImplementation((config: any) => {
        capturedCallback = config.callback;
      });
      const mockRenderButton = vi.fn();

      (window as any).google = {
        accounts: {
          id: {
            initialize: mockInitialize,
            renderButton: mockRenderButton,
          },
        },
      };

      service.initializeGoogleButton('google-btn', 'test-client-id');

      expect(mockInitialize).toHaveBeenCalledWith({
        client_id: 'test-client-id',
        callback: expect.any(Function),
      });

      expect(mockRenderButton).toHaveBeenCalledWith(mockElement, {
        theme: 'outline',
        size: 'large',
        text: 'continue_with',
        shape: 'pill',
        width: 318, // 350 - 32 = 318
      });

      // Invoke the callback to verify integration with handleGoogleCredentialResponse
      capturedCallback({ credential: 'test-token-from-google' });
      expect(handleResponseSpy).toHaveBeenCalledWith('test-token-from-google');

      document.body.removeChild(parentElement);
    });

    it('should render button with dark theme if data-theme attribute is dark', () => {
      const parentElement = document.createElement('div');
      const mockElement = document.createElement('div');
      mockElement.id = 'google-btn-dark';
      parentElement.appendChild(mockElement);
      document.body.appendChild(parentElement);

      document.documentElement.setAttribute('data-theme', 'dark');

      const mockInitialize = vi.fn();
      const mockRenderButton = vi.fn();

      (window as any).google = {
        accounts: {
          id: {
            initialize: mockInitialize,
            renderButton: mockRenderButton,
          },
        },
      };

      service.initializeGoogleButton('google-btn-dark', 'test-client-id');

      expect(mockRenderButton).toHaveBeenCalledWith(
        mockElement,
        expect.objectContaining({
          theme: 'filled_black',
        })
      );

      document.documentElement.removeAttribute('data-theme');
      document.body.removeChild(parentElement);
    });

    it('should not throw error if element is not found in DOM', () => {
      const mockInitialize = vi.fn();
      const mockRenderButton = vi.fn();

      (window as any).google = {
        accounts: {
          id: {
            initialize: mockInitialize,
            renderButton: mockRenderButton,
          },
        },
      };

      expect(() =>
        service.initializeGoogleButton('non-existent-id', 'test-client-id')
      ).not.toThrow();

      expect(mockInitialize).toHaveBeenCalled();
      expect(mockRenderButton).not.toHaveBeenCalled();
    });
  });

  describe('handleGoogleCredentialResponse', () => {
    it('should handle existing registered user login successfully', () => {
      const mockLoginResponse: LoginResponse = {
        accessToken: 'access-token-123',
        refreshToken: 'refresh-token-123',
        tokenType: 'Bearer',
        id: 'user-1',
        username: 'Mario',
        email: 'mario@example.com',
        role: 'USER',
      };

      const mockCheckResponse: GoogleCheckResponse = {
        registered: true,
        loginResponse: mockLoginResponse,
      };

      mockAuthService.checkGoogleUser.mockReturnValue(of(mockCheckResponse));

      service.handleGoogleCredentialResponse('mock-id-token');

      expect(service.isLoading()).toBe(false);
      expect(service.pendingIdToken()).toBe('mock-id-token');
      expect(mockAuthService.setSession).toHaveBeenCalledWith(mockLoginResponse);

      expect(mockToastService.show).toHaveBeenCalledWith(
        'Bentornato, Mario!',
        { classname: 'bg-success text-white' }
      );
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should handle unregistered user and set pending state', () => {
      const mockCheckResponse: GoogleCheckResponse = {
        registered: false,
        suggestedUsername: 'mario_suggested',
        email: 'mario@example.com',
      };

      mockAuthService.checkGoogleUser.mockReturnValue(of(mockCheckResponse));

      service.handleGoogleCredentialResponse('mock-id-token');

      expect(service.isLoading()).toBe(false);
      expect(service.pendingGoogleUser()).toEqual(mockCheckResponse);
      expect(service.selectedUsername()).toBe('mario_suggested');
    });

    it('should handle unregistered user without suggestedUsername and fallback to empty string', () => {
      const mockCheckResponse: GoogleCheckResponse = {
        registered: false,
        email: 'mario@example.com',
      };

      mockAuthService.checkGoogleUser.mockReturnValue(of(mockCheckResponse));

      service.handleGoogleCredentialResponse('mock-id-token');

      expect(service.isLoading()).toBe(false);
      expect(service.pendingGoogleUser()).toEqual(mockCheckResponse);
      expect(service.selectedUsername()).toBe('');
    });

    it('should handle error when checkGoogleUser fails', () => {
      mockAuthService.checkGoogleUser.mockReturnValue(
        throwError(() => new Error('API Error'))
      );

      service.handleGoogleCredentialResponse('mock-id-token');

      expect(service.isLoading()).toBe(false);
      expect(mockToastService.show).toHaveBeenCalledWith(
        'Autenticazione con Google fallita. Riprova.',
        { classname: 'bg-danger text-white' }
      );
    });
  });

  describe('completeGoogleRegistration', () => {
    it('should show warning toast if pendingIdToken or selectedUsername is empty', () => {
      service.pendingIdToken.set('');
      service.selectedUsername.set('   ');

      service.completeGoogleRegistration();

      expect(mockToastService.show).toHaveBeenCalledWith(
        'Inserisci un nome utente valido.',
        { classname: 'bg-warning text-dark' }
      );
      expect(mockAuthService.registerGoogleUser).not.toHaveBeenCalled();
    });

    it('should complete registration successfully and clear pending state', () => {
      service.pendingIdToken.set('valid-token');
      service.selectedUsername.set('mario_new');

      const mockLoginResponse: LoginResponse = {
        accessToken: 'new-access-token',
        refreshToken: 'new-refresh-token',
        tokenType: 'Bearer',
        id: 'user-2',
        username: 'mario_new',
        email: 'mario@example.com',
        role: 'USER',
      };

      mockAuthService.registerGoogleUser.mockReturnValue(of(mockLoginResponse));

      service.completeGoogleRegistration();

      expect(service.isLoading()).toBe(false);
      expect(service.pendingGoogleUser()).toBeNull();
      expect(service.pendingIdToken()).toBe('');
      expect(service.selectedUsername()).toBe('');

      expect(mockAuthService.registerGoogleUser).toHaveBeenCalledWith({
        idToken: 'valid-token',
        username: 'mario_new',
      });

      expect(mockToastService.show).toHaveBeenCalledWith(
        'Benvenuto su Beesagono, mario_new!',
        { classname: 'bg-success text-white' }
      );
      expect(mockRouter.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should handle 409 conflict error when username is already taken', () => {
      service.pendingIdToken.set('valid-token');
      service.selectedUsername.set('taken_username');

      mockAuthService.registerGoogleUser.mockReturnValue(
        throwError(() => ({ status: 409 }))
      );

      service.completeGoogleRegistration();

      expect(service.isLoading()).toBe(false);
      expect(mockToastService.show).toHaveBeenCalledWith(
        'Questo username è già stato preso.',
        { classname: 'bg-danger text-white' }
      );
    });

    it('should handle generic error on registration failure', () => {
      service.pendingIdToken.set('valid-token');
      service.selectedUsername.set('new_username');

      mockAuthService.registerGoogleUser.mockReturnValue(
        throwError(() => ({ status: 500 }))
      );

      service.completeGoogleRegistration();

      expect(service.isLoading()).toBe(false);
      expect(mockToastService.show).toHaveBeenCalledWith(
        'Registrazione fallita. Lo username potrebbe essere già in uso.',
        { classname: 'bg-danger text-white' }
      );
    });
  });

  describe('cancelGoogleRegistration', () => {
    it('should reset all pending signal states', () => {
      service.pendingGoogleUser.set({ registered: false });
      service.pendingIdToken.set('some-token');
      service.selectedUsername.set('some-username');

      service.cancelGoogleRegistration();

      expect(service.pendingGoogleUser()).toBeNull();
      expect(service.pendingIdToken()).toBe('');
      expect(service.selectedUsername()).toBe('');
    });
  });
});