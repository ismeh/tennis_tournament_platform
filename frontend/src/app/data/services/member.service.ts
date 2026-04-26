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
  private readonly baseUrl = AppSettings.API_URL.replace(/\/api\/?$/, '');
  private readonly profileUrl = `${AppSettings.API_URL}/auth/profile`;

  getMemberByEmail(email: string): Observable<MemberResponse> {
    return this.http.get<MemberResponse>(`${this.baseUrl}/members/${encodeURIComponent(email)}`);
  }

  getMyProfile(): Observable<ProfileResponse> {
    return this.http.get<ProfileResponse>(this.profileUrl);
  }

  updateMyProfile(payload: ProfileRequest): Observable<ProfileResponse> {
    return this.http.put<ProfileResponse>(this.profileUrl, payload);
  }
}