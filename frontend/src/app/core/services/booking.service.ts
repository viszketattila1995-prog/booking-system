import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { BookingResponse } from '../models/booking.models';

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  book(timeSlotId: string): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(`${this.apiBaseUrl}/time-slots/${timeSlotId}/book`, {});
  }

  listMine(): Observable<BookingResponse[]> {
    return this.http.get<BookingResponse[]>(`${this.apiBaseUrl}/bookings/me`);
  }

  cancel(bookingId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/bookings/${bookingId}`);
  }
}
