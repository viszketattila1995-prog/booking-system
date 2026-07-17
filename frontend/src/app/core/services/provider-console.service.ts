import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { ApplyProviderRequest, ProviderResponse } from '../models/provider.models';
import {
  CreateServiceOfferingRequest,
  ServiceOfferingResponse,
  UpdateServiceOfferingRequest,
} from '../models/service-offering.models';
import { CreateTimeSlotRequest, TimeSlotResponse } from '../models/time-slot.models';

// A bejelentkezett user SAJÁT provider-jelentkezésének és -erőforrásainak
// kezelése (apply, service offering/time slot CRUD) - a public böngészést
// (ProviderService) szándékosan külön tartja, mert más a hívó (guest vs.
// saját provider) és más a szükséges jogosultság.
@Injectable({ providedIn: 'root' })
export class ProviderConsoleService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  // A backend 204-et ad, ha a user még sosem jelentkezett - ez nem hiba,
  // csak azt jelzi, hogy az apply űrlapot kell mutatni.
  getMine(): Observable<ProviderResponse | null> {
    return this.http
      .get<ProviderResponse>(`${this.apiBaseUrl}/providers/me`, { observe: 'response' })
      .pipe(map((response) => response.body));
  }

  apply(request: ApplyProviderRequest): Observable<ProviderResponse> {
    return this.http.post<ProviderResponse>(`${this.apiBaseUrl}/providers/apply`, request);
  }

  listMyServiceOfferings(): Observable<ServiceOfferingResponse[]> {
    return this.http.get<ServiceOfferingResponse[]>(`${this.apiBaseUrl}/providers/me/service-offerings`);
  }

  createServiceOffering(request: CreateServiceOfferingRequest): Observable<ServiceOfferingResponse> {
    return this.http.post<ServiceOfferingResponse>(`${this.apiBaseUrl}/providers/me/service-offerings`, request);
  }

  updateServiceOffering(id: string, request: UpdateServiceOfferingRequest): Observable<ServiceOfferingResponse> {
    return this.http.put<ServiceOfferingResponse>(`${this.apiBaseUrl}/providers/me/service-offerings/${id}`, request);
  }

  deleteServiceOffering(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/providers/me/service-offerings/${id}`);
  }

  listTimeSlots(serviceOfferingId: string): Observable<TimeSlotResponse[]> {
    return this.http.get<TimeSlotResponse[]>(`${this.apiBaseUrl}/service-offerings/${serviceOfferingId}/time-slots`);
  }

  createTimeSlot(serviceOfferingId: string, request: CreateTimeSlotRequest): Observable<TimeSlotResponse> {
    return this.http.post<TimeSlotResponse>(
      `${this.apiBaseUrl}/providers/me/service-offerings/${serviceOfferingId}/time-slots`,
      request,
    );
  }

  cancelTimeSlot(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/providers/me/time-slots/${id}`);
  }
}
