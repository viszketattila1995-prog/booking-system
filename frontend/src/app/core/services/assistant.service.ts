import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { ChatMessage, ChatResponse } from '../models/assistant.models';

@Injectable({ providedIn: 'root' })
export class AssistantService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = inject(API_BASE_URL);

  chat(messages: ChatMessage[]): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.apiBaseUrl}/assistant/chat`, { messages });
  }
}
