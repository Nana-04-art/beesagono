import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth/auth.service';

export const headerInterceptor: HttpInterceptorFn = (request, next) => {
    const authService = inject(AuthService);
    const token = localStorage.getItem('token');

    // Base headers setup
    const headers: Record<string, string> = {
        'Accept': 'application/json'
    };

    if (token && token !== 'null') {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const clonedRequest = request.clone({ setHeaders: headers });

    return next(clonedRequest).pipe(
        catchError((error: HttpErrorResponse) => {
            const isLoginRequest = request.url.includes('/api/auth/login');

            if (error.status === 401 && !isLoginRequest) {
                console.error('Token expired or invalid - Globally intercepted');

                // Clears both LocalStorage and reactive Signals (currentUser, token, etc.)
                // and redirects to /login
                authService.clearSession();
            }

            // Always rethrow the error for component-level handlers
            return throwError(() => error);
        })
    );
};