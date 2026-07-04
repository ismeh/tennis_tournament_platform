import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AppSettings } from '../../shared/constants';

export interface InvitationPreview {
  tournamentName: string;
  playerDisplayName: string;
  expired: boolean;
  claimed: boolean;
}

export interface InviteParticipantResponse {
  invitationUrl: string;
}

export interface ClaimInvitationResponse {
  message: string;
}

@Injectable({ providedIn: 'root' })
export class InvitationService {
  private readonly http = inject(HttpClient);
  private readonly PENDING_INVITATION_KEY = 'pending_invitation_token';

  previewInvitation(token: string): Observable<InvitationPreview> {
    return this.http.get<InvitationPreview>(`${AppSettings.API_URL}/invitations/preview`, {
      params: { token }
    });
  }

  generateInvitation(tournamentId: string, participantId: string): Observable<InviteParticipantResponse> {
    return this.http.post<InviteParticipantResponse>(
      `${AppSettings.API_URL}/tournaments/${tournamentId}/participants/${participantId}/invite`,
      {}
    );
  }

  claimInvitation(token: string): Observable<ClaimInvitationResponse> {
    return this.http.post<ClaimInvitationResponse>(`${AppSettings.API_URL}/auth/claim-invitation`, { token });
  }

  storePendingToken(token: string): void {
    sessionStorage.setItem(this.PENDING_INVITATION_KEY, token);
  }

  consumePendingToken(): string | null {
    const token = sessionStorage.getItem(this.PENDING_INVITATION_KEY);
    if (token) {
      sessionStorage.removeItem(this.PENDING_INVITATION_KEY);
    }
    return token;
  }

  hasPendingToken(): boolean {
    return !!sessionStorage.getItem(this.PENDING_INVITATION_KEY);
  }
}
