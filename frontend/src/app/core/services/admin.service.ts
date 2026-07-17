import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { ProviderResponse } from '../models/provider.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  listPendingProviders(): Observable<ProviderResponse[]> {
    return this.http.get<ProviderResponse[]>(`${this.apiBaseUrl}/admin/providers/pending`);
  }

  approve(providerId: string): Observable<ProviderResponse> {
    return this.http.post<ProviderResponse>(`${this.apiBaseUrl}/admin/providers/${providerId}/approve`, {});
  }

  reject(providerId: string): Observable<ProviderResponse> {
    return this.http.post<ProviderResponse>(`${this.apiBaseUrl}/admin/providers/${providerId}/reject`, {});
  }
}
