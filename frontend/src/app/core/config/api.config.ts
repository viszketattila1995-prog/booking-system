import { InjectionToken } from '@angular/core';

// Fejlesztéshez a backend külön porton fut (8080), a CORS-t a SecurityConfig
// már felkészítette a localhost:4200 origin-re - nincs szükség dev proxy-ra.
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => 'http://localhost:8080/api',
});
