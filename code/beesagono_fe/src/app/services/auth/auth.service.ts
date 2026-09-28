import { HttpClient } from '@angular/common/http';
import { Injectable, signal, computed, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import {
  GoogleCheckResponse,
  GoogleLoginRequest,
  GoogleRegisterRequest,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse
} from '../../models/auth/auth-dto.model';

export interface User {
  username: string;
  email?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = 'http://localhost:8080/api/auth';
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  // Reactive state using Angular Signals
  readonly currentUser = signal<User | null>(this.getUserFromStorage());
  readonly isLoggedIn = computed(() => !!this.currentUser() && !!localStorage.getItem('token'));

  private getUserFromStorage(): User | null {
    const username = localStorage.getItem('username');
    return username ? { username } : null;
  }

  // Helper method to update the local session
  public setSession(loginResponse: LoginResponse): void {
    localStorage.setItem('token', loginResponse.accessToken);
    if (loginResponse.refreshToken) {
      localStorage.setItem('refreshToken', loginResponse.refreshToken);
    }
    if (loginResponse.username) {
      localStorage.setItem('username', loginResponse.username);
      this.currentUser.set({ username: loginResponse.username });
    }
  }

  register(request: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.baseUrl}/register`, request);
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, request).pipe(
      tap((response) => this.setSession(response))
    );
  }

  logout(): void {
    this.http.post(`${this.baseUrl}/logout`, {}).subscribe({
      next: () => this.clearSession(),
      error: () => this.clearSession()
    });
  }

  public clearSession(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('username');
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  checkGoogleUser(request: GoogleLoginRequest): Observable<GoogleCheckResponse> {
    return this.http.post<GoogleCheckResponse>(`${this.baseUrl}/google/check`, request);
  }

  registerGoogleUser(request: GoogleRegisterRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/google/register`, request).pipe(
      tap((response) => this.setSession(response))
    );
  }
}