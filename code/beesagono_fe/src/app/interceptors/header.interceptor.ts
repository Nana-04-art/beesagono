// header.interceptor.ts
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const headerInterceptor: HttpInterceptorFn = (request, next) => {
    const router = inject(Router);
    const token = localStorage.getItem('token');

    // Prepariamo gli header di base
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
                localStorage.removeItem('token');
                router.navigate(['/login']);
            }

            // Rilancia sempre l'errore per far scattare il blocco error: () del componente
            return throwError(() => error);
        })
    );
};