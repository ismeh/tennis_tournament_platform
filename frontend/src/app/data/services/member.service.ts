import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppSettings } from '../../shared/constants';
import { MemberResponse } from '../interfaces/member.model';

@Injectable({
  providedIn: 'root'
})
export class MemberService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = AppSettings.API_URL.replace(/\/api\/?$/, '');

  getMemberByEmail(email: string): Observable<MemberResponse> {
    return this.http.get<MemberResponse>(`${this.baseUrl}/members/${encodeURIComponent(email)}`);
  }
}