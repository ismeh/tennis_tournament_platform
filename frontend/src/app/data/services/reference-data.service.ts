import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { NationalityOption } from '../interfaces/reference-data.model';
import { AppSettings } from '../../shared/constants';

@Injectable({
  providedIn: 'root'
})
export class ReferenceDataService {
  private readonly http = inject(HttpClient);

  getNationalities(): Observable<NationalityOption[]> {
    return this.http.get<NationalityOption[]>(`${AppSettings.API_URL}/refs/nationalities`);
  }
}
