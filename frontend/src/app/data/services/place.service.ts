import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AppSettings } from '../../shared/constants';

export interface PlaceSuggestion {
  placeId: string;
  name: string;
  formattedAddress: string;
  latitude: number;
  longitude: number;
  mapsUrl: string;
}

@Injectable({
  providedIn: 'root'
})
export class PlaceService {
  private readonly http = inject(HttpClient);

  search(query: string, lat?: number | null, lng?: number | null): Observable<PlaceSuggestion[]> {
    let params = new HttpParams().set('query', query);
    if (lat !== undefined && lat !== null) {
      params = params.set('lat', String(lat));
    }
    if (lng !== undefined && lng !== null) {
      params = params.set('lng', String(lng));
    }
    return this.http.get<PlaceSuggestion[]>(`${AppSettings.API_URL}/places/search`, { params });
  }
}
