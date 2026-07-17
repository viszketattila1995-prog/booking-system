import { HttpErrorResponse } from '@angular/common/http';

export function extractErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error;
    if (body && typeof body === 'object') {
      if ('fieldErrors' in body && body.fieldErrors) {
        return Object.values(body.fieldErrors as Record<string, string>).join(', ');
      }
      if ('message' in body && typeof body.message === 'string') {
        return body.message;
      }
    }
  }
  return fallback;
}
