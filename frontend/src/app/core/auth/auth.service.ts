import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject, Observable, of, throwError, catchError, tap, finalize } from 'rxjs';
import {
  LoginRequest,
  LoginResponse,
  RefreshTokenResponse,
  RegisterRequest,
  RegisterResponse
} from './auth.model';
import { AppSettings } from '../../shared/constants';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly API_URL = `${AppSettings.API_URL}/auth`;
  private readonly TOKEN_KEY = AppSettings.TOKEN_KEY;
  private readonly REFRESH_TOKEN_KEY = AppSettings.REFRESH_TOKEN_KEY;
  private readonly USER_NAME_KEY = AppSettings.USER_NAME_KEY;

  private authStatus$ = new BehaviorSubject<boolean>(this.checkToken());
  private userDisplayName$ = new BehaviorSubject<string | null>(this.getInitialDisplayName());

  get isLoggedIn$(): Observable<boolean> {
    return this.authStatus$.asObservable();
  }

  get displayName$(): Observable<string | null> {
    return this.userDisplayName$.asObservable();
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API_URL}/login`, credentials).pipe(
      tap(response => {
        this.setSession(response.token, response.refreshToken);
      })
    );
  }

  register(payload: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.API_URL}/register`, payload).pipe(
      tap(response => {
        this.setSession(response.token, response.refreshToken, payload.name);
      })
    );
  }

  refreshToken(): Observable<RefreshTokenResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http
      .post<RefreshTokenResponse>(`${this.API_URL}/refresh`, { refreshToken })
      .pipe(
        tap(response => {
          const accessToken = response.accessToken;
          const nextRefreshToken = response.refreshToken ?? refreshToken;
          this.setSession(accessToken, nextRefreshToken);
        })
      );
  }

  logout(): Observable<void> {
    const refreshToken = this.getRefreshToken();
    const logoutRequest$: Observable<void> = refreshToken
      ? this.http.post<void>(`${this.API_URL}/logout`, { refreshToken })
      : of(void 0);

    return logoutRequest$.pipe(
      catchError(() => of(void 0)),
      finalize(() => {
        this.clearSession();
      })
    );
  }

  private setSession(token: string, refreshToken?: string, fallbackName?: string): void {
    const extractedName = this.extractDisplayNameFromToken(token);
    const resolvedName = extractedName ?? this.normalizeName(fallbackName ?? null);

    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.TOKEN_KEY, token);
      if (refreshToken) {
        localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
      }
      if (resolvedName) {
        localStorage.setItem(this.USER_NAME_KEY, resolvedName);
      } else {
        localStorage.removeItem(this.USER_NAME_KEY);
      }
    }

    this.authStatus$.next(true);
    this.userDisplayName$.next(resolvedName);
  }

  getToken(): string | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }

    return localStorage.getItem(this.TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }

    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  private checkToken(): boolean {
    if (!isPlatformBrowser(this.platformId)) {
      return false;
    }

    return !!localStorage.getItem(this.TOKEN_KEY);
  }

  private getInitialDisplayName(): string | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }

    const token = localStorage.getItem(this.TOKEN_KEY);
    if (token) {
      const extractedName = this.extractDisplayNameFromToken(token);
      if (extractedName) {
        localStorage.setItem(this.USER_NAME_KEY, extractedName);
        return extractedName;
      }
    }

    return this.normalizeName(localStorage.getItem(this.USER_NAME_KEY));
  }

  private extractDisplayNameFromToken(token: string): string | null {
    const payload = this.decodeJwtPayload(token);
    if (!payload) {
      return null;
    }

    const rawName = payload['name'] ?? payload['preferred_username'] ?? payload['sub'];
    if (typeof rawName !== 'string') {
      return null;
    }

    const fromEmail = rawName.includes('@') ? rawName.split('@')[0] : rawName;
    return this.normalizeName(fromEmail);
  }

  private decodeJwtPayload(token: string): Record<string, unknown> | null {
    const tokenParts = token.split('.');
    if (tokenParts.length !== 3) {
      return null;
    }

    try {
      const base64 = tokenParts[1].replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
      const decoded = atob(padded);
      return JSON.parse(decoded) as Record<string, unknown>;
    } catch {
      return null;
    }
  }

  private normalizeName(value: string | null): string | null {
    if (!value) {
      return null;
    }

    const trimmed = value.trim();
    if (!trimmed) {
      return null;
    }

    return trimmed;
  }

  private clearSession(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(this.TOKEN_KEY);
      localStorage.removeItem(this.REFRESH_TOKEN_KEY);
      localStorage.removeItem(this.USER_NAME_KEY);
    }

    this.authStatus$.next(false);
    this.userDisplayName$.next(null);
  }
}