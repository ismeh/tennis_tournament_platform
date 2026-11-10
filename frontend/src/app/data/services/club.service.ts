import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppSettings } from '../../shared/constants';
import {
  ClubMemberResponse,
  ClubMembershipResponse,
  ClubManagementResponse,
  ClubPageResponse,
  ClubPermission,
  ClubProfileRequest,
  ClubRankingResponse,
  ClubResponse,
  ClubRole,
  ClubRolePermissionsResponse,
  ClubTournamentSummary,
} from '../interfaces/club.model';

@Injectable({
  providedIn: 'root',
})
export class ClubService {
  private readonly http = inject(HttpClient);

  searchClubs(query: string): Observable<ClubResponse[]> {
    return this.http.get<ClubResponse[]>(`${AppSettings.API_URL}/clubs`, {
      params: { q: query },
    });
  }

  listPublicClubs(query?: string | null): Observable<ClubResponse[]> {
    let params = new HttpParams();
    if (query?.trim()) {
      params = params.set('q', query.trim());
    }
    return this.http.get<ClubResponse[]>(`${this.apiUrl}/public`, { params });
  }

  getPublicClub(slug: string): Observable<ClubPageResponse> {
    return this.http.get<ClubPageResponse>(`${this.apiUrl}/public/${encodeURIComponent(slug)}`);
  }

  getPublicRanking(slug: string, gender?: string | null): Observable<ClubRankingResponse[]> {
    let params = new HttpParams();
    if (gender) {
      params = params.set('gender', gender);
    }
    return this.http.get<ClubRankingResponse[]>(
      `${this.apiUrl}/public/${encodeURIComponent(slug)}/rankings`,
      { params },
    );
  }

  getPublicTournaments(slug: string): Observable<ClubTournamentSummary[]> {
    return this.http.get<ClubTournamentSummary[]>(
      `${this.apiUrl}/public/${encodeURIComponent(slug)}/tournaments`,
    );
  }

  getPermissionCatalog(): Observable<ClubRolePermissionsResponse[]> {
    return this.http.get<ClubRolePermissionsResponse[]>(`${this.apiUrl}/permissions`);
  }

  createClub(payload: ClubProfileRequest): Observable<ClubPageResponse> {
    return this.http.post<ClubPageResponse>(this.apiUrl, payload);
  }

  getMyClubs(): Observable<ClubMembershipResponse[]> {
    return this.http.get<ClubMembershipResponse[]>(`${this.apiUrl}/mine`);
  }

  getManagement(clubId: string): Observable<ClubManagementResponse> {
    return this.http.get<ClubManagementResponse>(`${this.apiUrl}/${clubId}/management`);
  }

  updatePage(clubId: string, payload: ClubProfileRequest): Observable<ClubPageResponse> {
    return this.http.put<ClubPageResponse>(`${this.apiUrl}/${clubId}/page`, payload);
  }

  getMembers(clubId: string): Observable<ClubMemberResponse[]> {
    return this.http.get<ClubMemberResponse[]>(`${this.apiUrl}/${clubId}/members`);
  }

  getManagementRanking(clubId: string, gender?: string | null): Observable<ClubRankingResponse[]> {
    let params = new HttpParams();
    if (gender) {
      params = params.set('gender', gender);
    }
    return this.http.get<ClubRankingResponse[]>(`${this.apiUrl}/${clubId}/rankings`, { params });
  }

  getManagementTournaments(clubId: string): Observable<ClubTournamentSummary[]> {
    return this.http.get<ClubTournamentSummary[]>(`${this.apiUrl}/${clubId}/tournaments`);
  }

  addMember(clubId: string, email: string, role: ClubRole): Observable<ClubMemberResponse> {
    return this.http.post<ClubMemberResponse>(`${this.apiUrl}/${clubId}/members`, { email, role });
  }

  updateMemberRole(
    clubId: string,
    membershipId: string,
    role: ClubRole,
  ): Observable<ClubMemberResponse> {
    return this.http.put<ClubMemberResponse>(
      `${this.apiUrl}/${clubId}/members/${membershipId}/role`,
      { role },
    );
  }

  updateMemberPermissions(
    clubId: string,
    membershipId: string,
    permissions: ClubPermission[],
  ): Observable<ClubMemberResponse> {
    return this.http.put<ClubMemberResponse>(
      `${this.apiUrl}/${clubId}/members/${membershipId}/permissions`,
      { permissions },
    );
  }

  removeMember(clubId: string, membershipId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${clubId}/members/${membershipId}`);
  }

  private get apiUrl(): string {
    return `${AppSettings.API_URL}/clubs`;
  }
}
