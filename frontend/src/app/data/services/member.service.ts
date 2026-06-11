import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppSettings } from '../../shared/constants';
import { MemberResponse, ProfileRequest, ProfileResponse } from '../interfaces/member.model';

@Injectable({
  providedIn: 'root'
})
export class MemberService {
  private readonly http = inject(HttpClient);

  getMemberByEmail(email: string): Observable<MemberResponse> {
    return this.http.get<MemberResponse>(`${AppSettings.API_URL}/members/${encodeURIComponent(email)}`);
  }

  getMyProfile(): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>(this.profileUrl);
  }

  updateMyProfile(payload: ProfileRequest): Observable<ProfileResponse> {
    return this.http.put<ProfileResponse>(this.profileUrl, payload);
  }

  private get profileUrl(): string {
    return `${AppSettings.API_URL}/auth/profile`;
  }
}
