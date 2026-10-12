import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { vi, describe, beforeEach, afterEach, it, expect } from 'vitest';
import { headerInterceptor } from './header.interceptor';
import { AuthService } from '../services/auth/auth.service';
import { LoginResponse } from '../models/auth/auth-dto.model';

describe('HeaderInterceptor & AuthService Integration (401 Handling)', () => {
    let http: HttpClient;
    let httpMock: HttpTestingController;
    let authService: AuthService;
    let routerMock: { navigate: ReturnType<typeof vi.fn> };

    // Complete helper object to comply with the LoginResponse interface
    const mockLoginResponse: LoginResponse = {
        accessToken: 'fake-jwt-token',
        refreshToken: 'fake-refresh-token',
        tokenType: 'Bearer',
        id: '1',
        username: 'Mario',
        email: 'mario@example.com',
        role: 'USER',
    };

    beforeEach(() => {
        routerMock = {
            navigate: vi.fn()
        };

        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(withInterceptors([headerInterceptor])),
                provideHttpClientTesting(),
                AuthService,
                { provide: Router, useValue: routerMock }
            ]
        });

        http = TestBed.inject(HttpClient);
        httpMock = TestBed.inject(HttpTestingController);
        authService = TestBed.inject(AuthService);

        localStorage.clear();
    });

    afterEach(() => {
        httpMock.verify();
        localStorage.clear();
    });

    it('should handle successful login, clear session, update Signals, and redirect to /login upon 401', () => {
        // Simulate successfully completed login via valid LoginResponse object
        authService.setSession(mockLoginResponse);

        // Verify initial state
        expect(authService.currentUser()).toEqual({ username: 'Mario' });
        expect(authService.isLoggedIn()).toBe(true);

        // User executes a protected HTTP request that receives a 401 response
        http.get('/api/puzzles/today').subscribe({
            next: () => expect.fail('Dovrebbe fallire con errore 401'),
            error: (error) => {
                expect(error.status).toBe(401);
            }
        });

        const req = httpMock.expectOne('/api/puzzles/today');
        req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

        // Verify that storage is cleared
        expect(localStorage.getItem('token')).toBeNull();
        expect(localStorage.getItem('username')).toBeNull();

        // Verify that reactive Signals are updated
        expect(authService.currentUser()).toBeNull();
        expect(authService.isLoggedIn()).toBe(false);

        // Verify automatic redirect to /login route
        expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('should not show authenticated user state if navigating back to game after 401', () => {
        // Simulate a previously active session destroyed by a 401 error
        authService.setSession({
            ...mockLoginResponse,
            accessToken: 'invalid-token',
            username: 'Luigi',
            email: 'luigi@example.com'
        });

        // Intercept 401
        http.get('/api/user/profile').subscribe({ error: () => { } });
        httpMock.expectOne('/api/user/profile').flush({}, { status: 401, statusText: 'Unauthorized' });

        // Simulate the check performed by a Guard or navigation menu when returning to /play
        const isUserLoggedIn = authService.isLoggedIn();
        const currentUser = authService.currentUser();

        expect(isUserLoggedIn).toBe(false);
        expect(currentUser).toBeNull();
    });
});