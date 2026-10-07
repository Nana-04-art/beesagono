import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { of, throwError } from 'rxjs';

import { AuthService } from './auth.service';
import { SyncService } from '../sync/sync.service';
import { GameFacadeService } from '../game/game-facade.service';
import {
  RegisterRequest,
  RegisterResponse,
  LoginRequest,
  LoginResponse,
  GoogleLoginRequest,
  GoogleCheckResponse,
  GoogleRegisterRequest,
} from '../../models/auth/auth-dto.model';
import { environment } from '../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: Router;

  let syncServiceMock: { syncLocalProgressWithServer: ReturnType<typeof vi.fn> };
  let gameFacadeMock: { loadDailyGame: ReturnType<typeof vi.fn> };

  const baseUrl = `${environment.apiBaseUrl}/auth`;

  const setupTestBed = () => {
    syncServiceMock = {
      syncLocalProgressWithServer: vi.fn().mockReturnValue(of({}))
    };

    gameFacadeMock = {
      loadDailyGame: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: SyncService, useValue: syncServiceMock },
        { provide: GameFacadeService, useValue: gameFacadeMock }
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);

    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  };

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('should be created', () => {
    setupTestBed();
    expect(service).toBeTruthy();
  });

  describe('Session Management & Signals', () => {
    it('should initialize currentUser from localStorage on startup', () => {
      localStorage.setItem('username', 'existinguser');

      setupTestBed();

      expect(service.currentUser()).toEqual({ username: 'existinguser' });
    });

    it('should update session, trigger sync and load daily game on setSession success', () => {
      setupTestBed();

      const mockLoginResponse: LoginResponse = {
        accessToken: 'access-123',
        refreshToken: 'refresh-123',
        tokenType: 'Bearer',
        id: '1',
        username: 'mario',
        email: 'mario@example.com',
        role: 'USER',
      };

      service.setSession(mockLoginResponse);

      expect(localStorage.getItem('token')).toBe('access-123');
      expect(localStorage.getItem('refreshToken')).toBe('refresh-123');
      expect(localStorage.getItem('username')).toBe('mario');
      expect(service.currentUser()).toEqual({ username: 'mario' });
      expect(service.isLoggedIn()).toBe(true);

      expect(syncServiceMock.syncLocalProgressWithServer).toHaveBeenCalled();
      expect(gameFacadeMock.loadDailyGame).toHaveBeenCalled();
    });

    it('should still load daily game on setSession if sync fails', () => {
      setupTestBed();

      syncServiceMock.syncLocalProgressWithServer.mockReturnValue(
        throwError(() => new Error('Sync failed'))
      );

      const mockLoginResponse: LoginResponse = {
        accessToken: 'access-123',
        refreshToken: 'refresh-123',
        tokenType: 'Bearer',
        id: '1',
        username: 'mario',
        email: 'mario@example.com',
        role: 'USER',
      };

      service.setSession(mockLoginResponse);

      expect(syncServiceMock.syncLocalProgressWithServer).toHaveBeenCalled();
      expect(gameFacadeMock.loadDailyGame).toHaveBeenCalled();
    });

    it('should clear session, load daily game, and navigate to /login on clearSession', () => {
      setupTestBed();

      localStorage.setItem('token', 'token-123');
      localStorage.setItem('refreshToken', 'refresh-123');
      localStorage.setItem('username', 'mario');

      service.clearSession();

      expect(localStorage.getItem('token')).toBeNull();
      expect(localStorage.getItem('refreshToken')).toBeNull();
      expect(localStorage.getItem('username')).toBeNull();
      expect(service.currentUser()).toBeNull();
      expect(service.isLoggedIn()).toBe(false);
      expect(gameFacadeMock.loadDailyGame).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('register', () => {
    beforeEach(() => setupTestBed());

    it('should send a POST request to register endpoint with user payload', () => {
      const mockRequest: RegisterRequest = {
        username: 'testuser',
        email: 'test@example.com',
        password: 'Password123!',
      };

      const mockResponse: RegisterResponse = {
        id: 'user-123',
        username: 'testuser',
        email: 'test@example.com',
        role: 'USER',
        message: 'User registered successfully',
      };

      service.register(mockRequest).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/register`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockRequest);

      req.flush(mockResponse);
    });

    it('should propagate error on failed registration', () => {
      const mockRequest: RegisterRequest = {
        username: 'existinguser',
        email: 'existing@example.com',
        password: 'Password123!',
      };

      service.register(mockRequest).subscribe({
        next: () => expect.fail('Should have failed with 400 error'),
        error: (error) => {
          expect(error.status).toBe(400);
          expect(error.statusText).toBe('Bad Request');
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/register`);
      req.flush('Username already taken', { status: 400, statusText: 'Bad Request' });
    });
  });

  describe('login', () => {
    beforeEach(() => setupTestBed());

    it('should send a POST request to login endpoint and trigger setSession', () => {
      const setSessionSpy = vi.spyOn(service, 'setSession');

      const mockRequest: LoginRequest = {
        usernameOrEmail: 'testuser',
        password: 'Password123!',
      };

      const mockResponse: LoginResponse = {
        accessToken: 'fake-access-token',
        refreshToken: 'fake-refresh-token',
        tokenType: 'Bearer',
        id: 'user-123',
        username: 'testuser',
        email: 'test@example.com',
        role: 'USER',
      };

      service.login(mockRequest).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockRequest);

      req.flush(mockResponse);

      expect(setSessionSpy).toHaveBeenCalledWith(mockResponse);
    });

    it('should propagate 401 error on invalid credentials', () => {
      const mockRequest: LoginRequest = {
        usernameOrEmail: 'wronguser',
        password: 'WrongPassword!',
      };

      service.login(mockRequest).subscribe({
        next: () => expect.fail('Should have failed with 401 error'),
        error: (error) => {
          expect(error.status).toBe(401);
          expect(error.statusText).toBe('Unauthorized');
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/login`);
      req.flush('Invalid credentials', { status: 401, statusText: 'Unauthorized' });
    });
  });

  describe('logout', () => {
    beforeEach(() => setupTestBed());

    it('should send a POST request to logout endpoint and clear session on success', () => {
      const clearSessionSpy = vi.spyOn(service, 'clearSession');

      service.logout();

      const req = httpMock.expectOne(`${baseUrl}/logout`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});

      req.flush({});

      expect(clearSessionSpy).toHaveBeenCalled();
    });

    it('should clear session even if logout API request fails', () => {
      const clearSessionSpy = vi.spyOn(service, 'clearSession');

      service.logout();

      const req = httpMock.expectOne(`${baseUrl}/logout`);
      req.flush('Logout error', { status: 500, statusText: 'Internal Server Error' });

      expect(clearSessionSpy).toHaveBeenCalled();
    });
  });

  describe('checkGoogleUser', () => {
    beforeEach(() => setupTestBed());

    it('should send a POST request to google check endpoint with Google payload', () => {
      const mockRequest: GoogleLoginRequest = {
        idToken: 'google-id-token',
      };

      const mockResponse: GoogleCheckResponse = {
        registered: true,
        email: 'google@example.com',
        loginResponse: {
          accessToken: 'fake-access-token',
          tokenType: 'Bearer',
          id: 'user-google-123',
          username: 'googleuser',
          email: 'google@example.com',
          role: 'USER',
        },
      };

      service.checkGoogleUser(mockRequest).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/google/check`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockRequest);

      req.flush(mockResponse);
    });

    it('should propagate error if google check fails', () => {
      const mockRequest: GoogleLoginRequest = {
        idToken: 'invalid-token',
      };

      service.checkGoogleUser(mockRequest).subscribe({
        next: () => expect.fail('Should have failed with 400 error'),
        error: (error) => {
          expect(error.status).toBe(400);
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/google/check`);
      req.flush('Invalid Google Token', { status: 400, statusText: 'Bad Request' });
    });
  });

  describe('registerGoogleUser', () => {
    beforeEach(() => setupTestBed());

    it('should send a POST request to google register endpoint and set session', () => {
      const setSessionSpy = vi.spyOn(service, 'setSession');

      const mockRequest: GoogleRegisterRequest = {
        idToken: 'google-id-token',
        username: 'googleuser',
      };

      const mockResponse: LoginResponse = {
        accessToken: 'fake-access-token',
        tokenType: 'Bearer',
        id: 'user-google-123',
        username: 'googleuser',
        email: 'google@example.com',
        role: 'USER',
      };

      service.registerGoogleUser(mockRequest).subscribe((response) => {
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${baseUrl}/google/register`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockRequest);

      req.flush(mockResponse);

      expect(setSessionSpy).toHaveBeenCalledWith(mockResponse);
    });

    it('should handle 409 conflict when username is already taken', () => {
      const mockRequest: GoogleRegisterRequest = {
        idToken: 'google-id-token',
        username: 'taken_username',
      };

      service.registerGoogleUser(mockRequest).subscribe({
        next: () => expect.fail('Should have failed with 409 error'),
        error: (error) => {
          expect(error.status).toBe(409);
        },
      });

      const req = httpMock.expectOne(`${baseUrl}/google/register`);
      req.flush('Username already taken', { status: 409, statusText: 'Conflict' });
    });
  });
});