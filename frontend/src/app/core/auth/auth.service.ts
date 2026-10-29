import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject, Observable, catchError, finalize, map, of, switchMap, tap, throwError } from 'rxjs';
import {
  LoginRequest,
  LoginResponse,
  RefreshTokenResponse,
  RegisterRequest,
  RegisterResponse,
  UserRole
} from './auth.model';
import { AppSettings } from '../../shared/constants';
import { ProfileResponse } from '../../data/interfaces/member.model';

export interface AccountExportData {
  account: {
    email: string;
    role: string;
    tier: string;
    registeredAt: string;
    privacyPolicyAccepted: boolean;
    privacyPolicyVersion: string;
    termsConditionsAccepted: boolean;
    termsConditionsVersion: string;
  };
  person: {
    firstName: string;
    lastName: string;
    nationality: string;
    birthDate: string;
    gender: string;
    tennisId: string;
  } | null;
  consentHistory: ConsentHistoryEntry[];
  participations: { tournamentName: string; eventName: string; entryStatus: string }[];
}

export interface ConsentHistoryEntry {
  documentType: string;
  action: string;
  createdAt: string;
}

export interface LegalDocument {
  documentType: string;
  version: string;
  contentHash: string;
  contentSnapshot: string;
  createdAt: string;
}

export interface ConsentHistoryResponse {
  history: ConsentHistoryEntry[];
}

export interface AccountDeletionResponse {
  message: string;
  processedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly TOKEN_KEY = AppSettings.TOKEN_KEY;
  private readonly REFRESH_TOKEN_KEY = AppSettings.REFRESH_TOKEN_KEY;
  private readonly USER_NAME_KEY = AppSettings.USER_NAME_KEY;
  private readonly USER_ROLE_KEY = AppSettings.USER_ROLE_KEY;
  private readonly USER_NATIONALITY_KEY = AppSettings.USER_NATIONALITY_KEY;

  private authStatus$ = new BehaviorSubject<boolean>(this.checkToken());
  private userDisplayName$ = new BehaviorSubject<string | null>(this.getInitialDisplayName());
  private userRole$ = new BehaviorSubject<UserRole | null>(this.getStoredRole());
  private userNationality$ = new BehaviorSubject<string | null>(this.getStoredNationality());

  get isLoggedIn$(): Observable<boolean> {
    return this.authStatus$.asObservable();
  }

  get displayName$(): Observable<string | null> {
    return this.userDisplayName$.asObservable();
  }

  get role$(): Observable<UserRole | null> {
    return this.userRole$.asObservable();
  }

  get nationality$(): Observable<string | null> {
    return this.userNationality$.asObservable();
  }

  get currentNationality(): string | null {
    return this.userNationality$.getValue();
  }

  get currentRole(): UserRole | null {
    return this.userRole$.getValue();
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.authUrl}/login`, credentials).pipe(
      tap(response => {
        this.setSession(response.accessToken, response.refreshToken);
        this.setRole(response.role);
      }),
      switchMap(response =>
        this.loadDisplayNameFromProfile().pipe(
          map(() => response),
          catchError(() => of(response))
        )
      )
    );
  }

  register(payload: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.authUrl}/register`, payload);
  }

  confirmEmail(token: string): Observable<RegisterResponse> {
    return this.http.get<RegisterResponse>(`${this.authUrl}/confirm-email`, {
      params: { token }
    });
  }

  resendConfirmation(email: string): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${this.authUrl}/resend-confirmation`, { email });
  }

  setDisplayName(name: string | null): void {
    const normalized = this.normalizeName(name);
    if (isPlatformBrowser(this.platformId)) {
      if (normalized) {
        localStorage.setItem(this.USER_NAME_KEY, normalized);
      } else {
        localStorage.removeItem(this.USER_NAME_KEY);
      }
    }

    this.userDisplayName$.next(normalized);
  }

  setNationality(nationality: string | null): void {
    const normalized = nationality?.trim() || null;
    if (isPlatformBrowser(this.platformId)) {
      if (normalized) {
        localStorage.setItem(this.USER_NATIONALITY_KEY, normalized);
      } else {
        localStorage.removeItem(this.USER_NATIONALITY_KEY);
      }
    }
    this.userNationality$.next(normalized);
  }

  loadDisplayNameFromProfile(): Observable<void> {
    if (!isPlatformBrowser(this.platformId)) {
      return of(void 0);
    }

    const token = this.readStoredToken();
    if (!token && !this.getRefreshToken()) {
      return of(void 0);
    }

    return this.getAccessTokenForRequest().pipe(
      switchMap(validToken => {
        if (!validToken) {
          return of(void 0);
        }
        return this.http.get<ProfileResponse>(`${this.authUrl}/profile`).pipe(
          tap(profile => {
            this.setDisplayName(this.resolveProfileDisplayName(profile));
            this.setNationality(profile.nationality ?? null);
            if (profile.role) {
              this.setRole(profile.role as UserRole);
            }
          }),
          map(() => void 0)
        );
      }),
      catchError(() => of(void 0))
    );
  }

  refreshToken(): Observable<RefreshTokenResponse> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http
      .post<RefreshTokenResponse>(`${this.authUrl}/refresh`, { refreshToken })
      .pipe(
        tap(response => {
          const accessToken = response.accessToken;
          const nextRefreshToken = response.refreshToken ?? refreshToken;
          this.setSession(accessToken, nextRefreshToken);
        }),
        switchMap(response =>
          this.loadDisplayNameFromProfile().pipe(
            map(() => response),
            catchError(() => of(response))
          )
        )
      );
  }

  getAccessTokenForRequest(): Observable<string | null> {
    if (!isPlatformBrowser(this.platformId)) {
      return of(null);
    }

    const token = this.readStoredToken();
    if (!token) {
      return of(null);
    }

    if (!this.shouldRefreshToken(token)) {
      return of(token);
    }

    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return this.logout().pipe(
        switchMap(() => throwError(() => new Error('No refresh token available')))
      );
    }

    return this.refreshToken().pipe(
      map(response => response.accessToken),
      catchError(error =>
        this.logout().pipe(
          switchMap(() => throwError(() => error))
        )
      )
    );
  }

  logout(): Observable<void> {
    const refreshToken = this.getRefreshToken();
    this.clearSession();

    const logoutRequest$: Observable<void> = refreshToken
      ? this.http.post<void>(`${this.authUrl}/logout`, { refreshToken })
      : of(void 0);

    return logoutRequest$.pipe(
      catchError(() => of(void 0)),
      finalize(() => this.clearSession())
    );
  }

  exportAccountData(): Observable<AccountExportData> {
    return this.http.get<AccountExportData>(`${this.authUrl}/account/export`);
  }

  deleteAccount(password: string): Observable<AccountDeletionResponse> {
    return this.http.delete<AccountDeletionResponse>(`${this.authUrl}/account`, {
      body: { password }
    });
  }

  updatePrivacyConsent(accepted: boolean, version: string): Observable<void> {
    return this.http.put<void>(`${this.authUrl}/account/consent`, {
      accepted,
      privacyPolicyVersion: version
    });
  }

  updateTermsConsent(accepted: boolean, version: string): Observable<void> {
    return this.http.put<void>(`${this.authUrl}/account/consent/terms`, {
      accepted,
      termsConditionsVersion: version
    });
  }

  getConsentHistory(): Observable<ConsentHistoryResponse> {
    return this.http.get<ConsentHistoryResponse>(`${this.authUrl}/account/consent/history`);
  }

  getLegalDocument(type: string): Observable<LegalDocument> {
    return this.http.get<LegalDocument>(`${AppSettings.API_URL}/legal/${type}/current`);
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

    const token = this.readStoredToken();
    if (!token) {
      return null;
    }

    return token;
  }

  getRefreshToken(): string | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }

    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  getCurrentUserEmail(): string | null {
    const token = this.getToken();
    if (!token) {
      return null;
    }

    const payload = this.decodeJwtPayload(token);
    if (!payload) {
      return null;
    }

    const rawEmail = payload['sub'] ?? payload['email'] ?? payload['preferred_username'];
    if (typeof rawEmail !== 'string') {
      return null;
    }

    const normalizedEmail = rawEmail.trim().toLowerCase();
    return normalizedEmail.includes('@') ? normalizedEmail : null;
  }

  private checkToken(): boolean {
    return this.hasValidStoredToken();
  }

  private getInitialDisplayName(): string | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }

    const token = this.readStoredToken();
    if (token && !this.isExpiredToken(token)) {
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

  private resolveProfileDisplayName(profile: ProfileResponse): string | null {
    const fullName = this.normalizeName([profile.firstName, profile.lastName].filter(Boolean).join(' '));
    if (fullName) {
      return fullName;
    }

    const emailPrefix = profile.email.includes('@') ? profile.email.split('@')[0] : profile.email;
    return this.normalizeName(emailPrefix);
  }

  private hasValidStoredToken(): boolean {
    if (!isPlatformBrowser(this.platformId)) {
      return false;
    }

    const token = this.readStoredToken();
    return !!token && !this.isExpiredToken(token);
  }

  private readStoredToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private shouldRefreshToken(token: string): boolean {
    const payload = this.decodeJwtPayload(token);
    if (!payload) {
      return false;
    }

    const expiration = payload['exp'];
    if (typeof expiration !== 'number') {
      return true;
    }

    const fiveMinutesInMs = 5 * 60 * 1000;
    return expiration * 1000 - Date.now() <= fiveMinutesInMs;
  }

  private isExpiredToken(token: string): boolean {
    const payload = this.decodeJwtPayload(token);
    if (!payload) {
      return true;
    }

    const expiration = payload['exp'];
    if (typeof expiration !== 'number') {
      return true;
    }

    return expiration * 1000 <= Date.now();
  }

  private setRole(role: UserRole): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem(this.USER_ROLE_KEY, role);
    }
    this.userRole$.next(role);
  }

  private getStoredRole(): UserRole | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }
    const stored = localStorage.getItem(this.USER_ROLE_KEY);
    if (stored === 'PLAYER' || stored === 'ORGANIZER' || stored === 'UMPIRE') {
      return stored;
    }
    return null;
  }

  private getStoredNationality(): string | null {
    if (!isPlatformBrowser(this.platformId)) {
      return null;
    }
    return localStorage.getItem(this.USER_NATIONALITY_KEY);
  }

  private clearSession(): void {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem(this.TOKEN_KEY);
      localStorage.removeItem(this.REFRESH_TOKEN_KEY);
      localStorage.removeItem(this.USER_NAME_KEY);
      localStorage.removeItem(this.USER_ROLE_KEY);
      localStorage.removeItem(this.USER_NATIONALITY_KEY);
    }

    this.authStatus$.next(false);
    this.userDisplayName$.next(null);
    this.userRole$.next(null);
    this.userNationality$.next(null);
  }

  private get authUrl(): string {
    return `${AppSettings.API_URL}/auth`;
  }

  private verifyTokensOnStartup(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    const token = this.readStoredToken();
    const refresh = this.getRefreshToken();

    // No tokens at all -> ensure logged out
    if (!token && !refresh) {
      this.clearSession();
      return;
    }

    // If access token is close to expiry or already expired and we have a refresh token,
    // attempt to refresh proactively so the app starts with a valid access token.
    try {
      if (token && this.shouldRefreshToken(token) && refresh) {
        this.refreshToken().subscribe({ next: () => { }, error: () => this.clearSession() });
        return;
      }

      if (token && this.isExpiredToken(token) && refresh) {
        this.refreshToken().subscribe({ next: () => { }, error: () => this.clearSession() });
        return;
      }

      // If token missing but refresh exists, try to refresh once.
      if (!token && refresh) {
        this.refreshToken().subscribe({ next: () => { }, error: () => this.clearSession() });
        return;
      }

      // If token present and valid, ensure observables reflect that state (in case storage changed)
      if (token && !this.isExpiredToken(token)) {
        this.authStatus$.next(true);
        const name = this.extractDisplayNameFromToken(token) ?? this.normalizeName(localStorage.getItem(this.USER_NAME_KEY));
        this.userDisplayName$.next(name);
      }
    } catch {
      this.clearSession();
    }
  }
}
