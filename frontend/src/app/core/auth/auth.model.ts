export type UserRole = 'PLAYER' | 'ORGANIZER' | 'UMPIRE';

export interface LoginResponse {
  accessToken: string;
  refreshToken?: string;
  role: UserRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name?: string;
  role: UserRole;
  privacyPolicyAccepted: boolean;
}

export interface RegisterResponse {
  emailVerificationRequired: boolean;
  message: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface RefreshTokenResponse {
  accessToken: string;
  refreshToken?: string;
}
