import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { ApplyProviderRequest, ProviderResponse } from '../models/provider.models';
import { ServiceOfferingResponse } from '../models/service-offering.models';
import { TimeSlotResponse } from '../models/time-slot.models';

@Injectable({ providedIn: 'root' })
export class ProviderService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  listApproved(): Observable<ProviderResponse[]> {
    return this.http.get<ProviderResponse[]>(`${this.apiBaseUrl}/providers`);
  }

  apply(request: ApplyProviderRequest): Observable<ProviderResponse> {
    return this.http.post<ProviderResponse>(`${this.apiBaseUrl}/providers/apply`, request);
  }

  listServiceOfferings(providerId: string): Observable<ServiceOfferingResponse[]> {
    return this.http.get<ServiceOfferingResponse[]>(`${this.apiBaseUrl}/providers/${providerId}/service-offerings`);
  }

  listTimeSlots(serviceOfferingId: string): Observable<TimeSlotResponse[]> {
    return this.http.get<TimeSlotResponse[]>(`${this.apiBaseUrl}/service-offerings/${serviceOfferingId}/time-slots`);
  }
}
